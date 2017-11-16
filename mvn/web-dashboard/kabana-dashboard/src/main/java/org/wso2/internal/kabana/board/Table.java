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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.annotation.Resource;
import javax.faces.bean.ManagedBean;
import javax.sql.DataSource;

/**
 * 
 * This is the call being called from the view
 * At the initialazion state get the data and prepare the table view
 *
 */

@ManagedBean(name = "table")
public class Table {

	private List<Row> rows = new ArrayList<Row>();
	private boolean dataLoaded = false;

	@Resource(name = "jdbc/kabana")
	private DataSource ds;
	private static final String QUERY = "SELECT c.ColumnName,c.Note,c.Title, p.Name, a.Login,a.Profile "
			+ "FROM Cards c " + "LEFT OUTER JOIN Projects p ON c.ProjectId = p.ProjectID "
			+ "LEFT OUTER JOIN Assignee a ON c.AssigneeId = a.AssigneeID " + "where p.State = 'OPEN' "
			+ "order by c.Backlog,c.Planned,c.USReady,c.USReviewed,c.DesignReviewed,c.InProgress,c.Blocked,"
			+ "c.CodeReviewed,c.SamplesDone,c.TestsAutomated,c.Done desc";

	/**
	 * 
	 * Call the DB and load the data view
	 * 
	 */
	public Table() {

		if(dataLoaded){
			return;
		}
		
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		//This is to temparary hold the rows and columns
		List<List<Card>>lTable = new ArrayList<List<Card>>();
		
		//Initalize 11 columns
		//Backlog, Planned, User Stories (Ready), User Stories (Reviewed), Design Reviewed, In Progress, Blocked, Code Reviewed, Samples Done, Tests Automated, Done
		for(int i = 0;i<11;i++){
			lTable.add(new ArrayList<>());	
		}
				
		try {
			Context ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/kabana");
		} catch (NamingException e1) {
			e1.printStackTrace();
			return;
		}
			
		//Call db and fill card objects
		try (Connection con = ds.getConnection()){
			preparedStatement = con.prepareStatement(QUERY);
			resultSet = preparedStatement.executeQuery();	
			while(resultSet.next()){
				Card card = new Card();
				// Order of the select query
				// c.ColumnName,c.Note,c.Title, p.Name, a.Login,a.Profile
				card.setColumnName(resultSet.getString(1));
				card.setNote(resultSet.getString(2));
				card.setTitle(resultSet.getString(3));
				card.setProjectName(resultSet.getString(4));
				card.setAssigneeName(resultSet.getString(5));
				card.setAssigneeProfile(resultSet.getString(6));
				lTable = addCell(card, lTable);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				preparedStatement.close();
			} catch (SQLException e) {				
				e.printStackTrace();
			}
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
		//Devide the cards into columns
		int iRowSize = getRowSize(lTable);
		for(int i = 1;i <= iRowSize ;i++){
			Row row = new Row();
			row.setColumn1(getCard(lTable.get(0), i));
			row.setColumn2(getCard(lTable.get(1), i));
			row.setColumn3(getCard(lTable.get(2), i));
			row.setColumn4(getCard(lTable.get(3), i));
			row.setColumn5(getCard(lTable.get(4), i));
			row.setColumn6(getCard(lTable.get(5), i));
			row.setColumn7(getCard(lTable.get(6), i));
			row.setColumn8(getCard(lTable.get(7), i));
			row.setColumn9(getCard(lTable.get(8), i));
			row.setColumn10(getCard(lTable.get(9), i));
			row.setColumn11(getCard(lTable.get(10), i));
			rows.add(row);
		}
		dataLoaded = true;
	}
	
	/**
	 * 
	 * See if there are any card for the given row for the given column
	 * If not retun a dummy card with no display style
	 * 
	 * @param lCards
	 * @param iIIndex
	 * @return
	 */
	private Card getCard(List<Card>lCards, int iIIndex){
		if(lCards.size() > iIIndex){
			return lCards.get(iIIndex);
		} else {
			return new Card(false);
		}
	}
	
	/**
	 * 
	 * Find maxmum number of row to draw
	 * 
	 * @param lTable
	 * @return
	 */
	private int getRowSize(List<List<Card>>lTable) {
		int rowSize = 0;
		for(List<Card> lColumn:lTable){
			int iSize = lColumn.size();
			if(iSize > rowSize){
				rowSize = iSize;
			}
		}
		return rowSize;
	}

	/**
	 * 
	 * Add card to relavant column
	 * 
	 * @param card
	 * @param lTable
	 * @return
	 */
	private List<List<Card>> addCell(Card card, List<List<Card>>lTable){
		switch (card.getColumnName()) {
		case "Backlog":
			lTable.get(0).add(card);
			break;
		case "Planned":
			lTable.get(1).add(card);
			break;
		case "User Stories (Ready)":
			lTable.get(2).add(card);
			break;
		case "User Stories (Reviewed)":
			lTable.get(3).add(card);
			break;
		case "Design Reviewed":
			lTable.get(4).add(card);
			break;
		case "In Progress":
			lTable.get(5).add(card);
			break;
		case "Blocked":
			lTable.get(6).add(card);
			break;
		case "Code Reviewed":
			lTable.get(7).add(card);
			break;
		case "Samples Done":
			lTable.get(8).add(card);
			break;
		case "Tests Automated":
			lTable.get(9).add(card);
			break;
		case "Done":
			lTable.get(10).add(card);
			break;
		default:
			lTable.get(0).add(card);
			break;
		}
		return lTable;
	}
	
	/***
	 * 
	 * @return
	 */
	public List<Row> getRows() {
		return rows;
	}
}