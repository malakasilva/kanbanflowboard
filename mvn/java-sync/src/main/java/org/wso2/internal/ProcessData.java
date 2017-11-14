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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.FileInputStream;

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

public class ProcessData {

	private static final String GIT_URL = "https://api.github.com/";
	private static final String GIT_PREVIEW_HEADER = "application/vnd.github.inertia-preview+json";
	private WriteData writeData;
    private String basicAuth;
	
	/**
	 * Start the main process with this method
	 */
	public void readWriteData() {
		// Read property file
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("config.properties");
			// load a properties file
			prop.load(input);
			writeData = new WriteData("org.gjt.mm.mysql.Driver", prop.getProperty("database"),
					prop.getProperty("dbuser"), prop.getProperty("dbpassword"));
			basicAuth = prop.getProperty("gituser");
		} catch (IOException ex) {
			ex.printStackTrace();
			writeData = new WriteData();
			basicAuth = "a2FiYW5hd3NvMjp3c28yMTIzNA==";
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Manage Projects
		// Using the API https://api.github.com/orgs/wso2/projects
		List<GitProject> gitProjects = readProjectData("wso2");
		gitProjects.addAll(readProjectData("wso2-extensions"));

		writeProjectData(gitProjects);

		// Find Project Columns and Cards
		// Using the API https://api.github.com/projects/<project_id>/columns
		for (GitProject gitProject : gitProjects) {
			//Process only the oprn projects
			if("OPEN".equals(gitProject.getState())){
				readColumns(gitProject);
			}
		}
		writeData.close();
	}

	/**
	 * 
	 * Reads columns and process the cards under each column
	 * 
	 * @param gitProject
	 */
	private void readColumns(GitProject gitProject) {
		// Get Columns and read cards
		String urlFragment = "projects/" + gitProject.getId() + "/columns";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jColumns = (JSONArray) phrase(invokeApi(httpGet));

		for (Iterator<JSONObject> iColumns = jColumns.iterator(); iColumns.hasNext();) {
			JSONObject jColumn = iColumns.next();
			// State of the project. This can be one of following
			// Backlog,Planned,User Stories (Ready),User Stories (Reviewed),Design Reviewed,
			// In Progress,Blocked,Code Reviewed,Samples Done,Tests Automated,Done
			String sColumnName = getString(jColumn.get("name"));
			String sColumnId = getString(jColumn.get("id"));
			readWriteCards(gitProject.getId(), sColumnId, sColumnName);
		}
	}

	/**
	 * 
	 * Main task is to manage the cards and assignees
	 * 
	 * @param sProjectId
	 * @param sColumnId
	 * @param sColumnName
	 * 
	 */
	private void readWriteCards(String sProjectId, String sColumnId, String sColumnName) {
		// Get all cards in a column
		String urlFragment = "projects/columns/" + sColumnId + "/cards";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jCards = (JSONArray) phrase(invokeApi(httpGet));
		for (Iterator<JSONObject> iCards = jCards.iterator(); iCards.hasNext();) {
			JSONObject jCard = iCards.next();
			GitCard gitCard = new GitCard();
			gitCard.setProjectId(sProjectId);
			gitCard.setColumn(sColumnName);
			gitCard.setCardId(jCard.get("id").toString());
			gitCard.setNote(getString(jCard.get("note")));
			// get the issue
			if (jCard.get("content_url") != null) {
				httpGet = new HttpGet(jCard.get("content_url").toString());
				JSONObject jIssue = (JSONObject) phrase(invokeApi(httpGet));
				gitCard.setIssueId(jIssue.get("id").toString());
				gitCard.setTitle(getString(jIssue.get("title")));

				JSONObject jAssignee = ((JSONObject) jIssue.get("assignee"));
				if (jAssignee != null && jAssignee.get("id") != null) {
					String assigneeId = jAssignee.get("id").toString();
					gitCard.setAssigneeId(assigneeId);

					Map<String, String> mColumns = new HashMap<String, String>();
					mColumns.put("AssigneeID", assigneeId);
					mColumns.put("Login", jAssignee.get("login").toString());
					mColumns.put("Profile", getString(jAssignee.get("avatar_url")));
					writeData.upsertRecord("AssigneeID", "Assignee", mColumns);
				}
			}
			writeData.manageRecords(gitCard);
		}
	}

	/**
	 * 
	 * Handle null json values
	 * 
	 * @param obj
	 * @return
	 */
	private String getString(Object obj) {
		if (obj != null) {
			return obj.toString();
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
		List<GitProject> lGitProjects = new ArrayList<GitProject>();
		String urlFragment = "orgs/" + orgName + "/projects";
		HttpGet httpGet = new HttpGet(GIT_URL + urlFragment);
		JSONArray jprojects = (JSONArray) phrase(invokeApi(httpGet));

		for (Iterator<JSONObject> iProjects = jprojects.iterator(); iProjects.hasNext();) {
			JSONObject jproject = iProjects.next();
			GitProject gitProject = new GitProject();
			gitProject.setName(jproject.get("name").toString());
			gitProject.setId(jproject.get("id").toString());
			gitProject.setState(jproject.get("state").toString());
			lGitProjects.add(gitProject);
		}
		return lGitProjects;
	}

	/**
	 * 
	 * @param lGitProjects
	 */
	private void writeProjectData(List<GitProject> lGitProjects) {
		for (GitProject gitProject : lGitProjects) {
			Map<String, String> mColumns = new HashMap<String, String>();
			mColumns.put("ProjectID", gitProject.getId());
			mColumns.put("Name", gitProject.getName());
			mColumns.put("State", gitProject.getState());
			writeData.upsertRecord("ProjectID", "Projects", mColumns);
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

		// Set Basic Authentication details to header
		httpRequestBase.addHeader("ACCEPT", GIT_PREVIEW_HEADER);
		httpRequestBase.addHeader("AUTHORIZATION", "Basic " + basicAuth);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse httpResponse = httpClient.execute(httpRequestBase)) {
			int responseCode = httpResponse.getStatusLine().getStatusCode();
			if (responseCode == 200) {
				// success
				try (BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8))) {
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
			if (object instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) object;
				return jsonArray;
			} else if (object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject) object;
				return jsonObject;
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

}
