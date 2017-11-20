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
package org.wso2.internal.kabana.board;

public class Card {

    private String columnName; 
    private String note;
    private String title; 
    private String projectName;
    private String assigneeName;
    private String assigneeProfile;
    private String style;
    private String issueLink;
    

    public Card(){
    	
    }
    
    public Card(boolean display){
    	if(!display){
    		style = "display:none";
    	}
    }
    
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getAssigneeName() {
		return assigneeName;
	}
	public void setAssigneeName(String assigneeName) {
		this.assigneeName = assigneeName;
	}
	public String getAssigneeProfile() {
		return assigneeProfile;
	}
	public void setAssigneeProfile(String assigneeProfile) {
		this.assigneeProfile = assigneeProfile;
	}
  	public String getStyle() {
		return style;
	}
	public void setStyle(String style) {
		this.style = style;
	}
	
	public String getIssueLink() {
		return issueLink;
	}

	public void setIssueLink(String issueLink) {
		this.issueLink = issueLink;
	}

	public String getDisplay(){
		String rtnString = "";
  		if(title != null && !title.trim().equals("")){
  			rtnString = title;
  		} else if (note != null) {
  			rtnString = note;
  		}
  		if(rtnString.length() > 70) {
  			rtnString = rtnString.substring(0, 65) + "...";	
  		}
  		return rtnString;
  	}
}
