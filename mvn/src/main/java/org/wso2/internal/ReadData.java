/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.internal.git.dto.GitCard;
import org.wso2.internal.git.dto.GitProject;

/**
 * 
 * This class is used to get data from GIT using rest api V3
 * 
 */

public class ReadData {

	private static final String GIT_URL =  "https://api.github.com/";
	private static final String GIT_PREVIEW_HEADER = "application/vnd.github.inertia-preview+json";
	private WriteData writeData = new WriteData();
	
	public void readWriteData() {
        String urlFragment =  "orgs/wso2/projects";
        urlFragment =  "projects/1032925";
        urlFragment =  "projects/1032925/columns";
        urlFragment =  "projects/columns/1711924/cards";
        urlFragment =  "repos/wso2/wum-client/issues/250";
        //Manage Projects
        //Using the API https://api.github.com/orgs/wso2/projects
        List<GitProject> gitProjects = readProjectData("wso2");
        writeProjectData(gitProjects);
        
        //Find Project Columns and Cards
        //Using the API https://api.github.com/projects/<project_id>/columns
        for(GitProject gitProject:gitProjects) {
        	readColumns(gitProject);
        }
        
	}

	
	private void readColumns(GitProject gitProject) {
		//Get Columns and read cards
		String urlFragment =  "projects/" + gitProject.getId() + "/columns";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jColumns = (JSONArray) phrase(invokeApi(httpGet));
		
		for(Iterator<JSONObject>iColumns = jColumns.iterator(); iColumns.hasNext();) {
			JSONObject jColumn = iColumns.next();
			//State of the project. This can be one of following
			//Backlog,Planned,User Stories (Ready),User Stories (Reviewed),Design Reviewed,
			//In Progress,Blocked,Code Reviewed,Samples Done,Tests Automated,Done
			String sColumnName = jColumn.get("name").toString();
			String sColumnId = jColumn.get("id").toString();
			readCards(gitProject.getId(), sColumnId, sColumnName);
		}
	}
	
	private void readCards(String sProjectId, String sColumnId, String sColumnName) {
		//Get all cards in a column
		String urlFragment =  "projects/columns/" + sColumnId + "/cards";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jCards = (JSONArray) phrase(invokeApi(httpGet));
		for(Iterator<JSONObject>iCards = jCards.iterator(); iCards.hasNext();) {
			JSONObject jCard = iCards.next();
			GitCard gitCard = new GitCard();
			gitCard.setProjectId(sProjectId);
			gitCard.setColumn(sColumnName);
			gitCard.setCardId(jCard.get("id").toString());
			gitCard.setNote(getString(jCard.get("note")));
			//get the issue
			if(jCard.get("content_url") != null) {				
				httpGet = new HttpGet(jCard.get("content_url").toString());
				JSONObject jIssue = (JSONObject) phrase(invokeApi(httpGet));
				gitCard.setIssueId(jIssue.get("id").toString());
				gitCard.setTitle(jIssue.get("title").toString());
				
				JSONObject jAssignee = ((JSONObject)jIssue.get("assignee"));
				if(jAssignee != null && jAssignee.get("id") != null) {
					String assigneeId = jAssignee.get("id").toString();
					gitCard.setAssigneeId(assigneeId);					
					
					Map<String,String>mColumns = new HashMap<String,String>();
					mColumns.put("AssigneeID", assigneeId);
					mColumns.put("Login", jAssignee.get("login").toString());
					mColumns.put("Profile", jAssignee.get("avatar_url").toString());
					writeData.upsertrecord("AssigneeID", "Assignee", mColumns);					
				}
			}	
			writeData.ManageRecords(gitCard);
		}
	}
	
	
	
	private String getString(Object obj) {
		if(obj != null) {
			obj.toString();
		}
		return "";
	}
	
	/**
	 * 
	 * Get all the git projects for given org
	 * 
	 * @param orgName
	 * @return
	 */
	private List<GitProject> readProjectData(String orgName) {
		List<GitProject>lGitProjects = new ArrayList<GitProject>();
		String urlFragment =  "orgs/" + orgName + "/projects";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jprojects = (JSONArray) phrase(invokeApi(httpGet));
		
		for(Iterator<JSONObject>iProjects = jprojects.iterator(); iProjects.hasNext();) {
			JSONObject jproject = iProjects.next();
			GitProject gitProject = new GitProject(); 
			gitProject.setName(jproject.get("name").toString());
			gitProject.setId(jproject.get("id").toString());
			gitProject.setState(jproject.get("state").toString());
			lGitProjects.add(gitProject);
		}
		return lGitProjects;
	}
	
	private void writeProjectData(List<GitProject>lGitProjects) {
		
		for(GitProject gitProject:lGitProjects) {
			Map<String,String>mColumns = new HashMap<String,String>();
			mColumns.put("ProjectID", gitProject.getId());
			mColumns.put("Name", gitProject.getName());
			mColumns.put("State", gitProject.getState());
			writeData.upsertrecord("ProjectID", "Projects", mColumns);
		}
		
	}
	
	/**
	 * 
	 * Do the actual callout and get the response
	 * 
	 * @param httpRequestBase 
	 * @return
	 */
    private String invokeApi(HttpRequestBase httpRequestBase) {
    	
    	//Set Basic Authentication details to header
    	httpRequestBase.addHeader("ACCEPT", GIT_PREVIEW_HEADER);
    	httpRequestBase.addHeader("AUTHORIZATION", "Basic " + "bWFsYWthc2lsdmE6c2V0MUBzZXQ=");
    	
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(httpRequestBase)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                //success
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity()
                        .getContent(), StandardCharsets.UTF_8))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    // return a JSON String from the response
                    return stringBuilder.toString();
                }
            } else {
            	System.err.println("Did not recive 200 status for the callout");            	
            }
        } catch (ClientProtocolException e) {
        	System.err.println(e);        	
        } catch (IOException e) {
        	System.err.println(e);        	
        }
        return null;
    }
	
    /**
     * 
     * Convert the JSON String to JSON Array Or object for processing
     * 
     * @param jsonText
     * @return
     */
    private Object phrase(String jsonText) {
        JSONParser parser = new JSONParser();        
		try {
			Object object = parser.parse(jsonText);
			if(object instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray)object;
				System.out.println(jsonArray.toJSONString());
				return jsonArray;
			} else if(object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)object;	
				System.out.println(jsonObject.toJSONString());
				return jsonObject;
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return null;    	
    }
    
}
