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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.wso2.internal.git.dto.GitCard;

/**
 * 
 * Class is used to do database operations
 * 
 *
 */

public class WriteData {

	private Connection connection;
    private Long currentTimestamp;
	
	/**
	 * 
	 * @param databaseDriver
	 * @param databaseUrl
	 * @param userName
	 * @param password
	 */
	public WriteData(String databaseDriver, String databaseUrl, String userName, String password) {
		try {
			// create a java (mysql) database connection
			Class.forName(databaseDriver);
			connection = DriverManager.getConnection(databaseUrl, userName, password);
			currentTimestamp = (new Date()).getTime();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			connection = null;
		}
	}

	/**
	 * 
	 */
	public WriteData() {
		this("org.gjt.mm.mysql.Driver", "jdbc:mysql://localhost/kabana", "root", "root");
	}

	/**
	 * 
	 * Handle Cards with timestamp update when moving from column to column
	 * 
	 * @param gitCard
	 * @return
	 */
	public int manageRecords(GitCard gitCard) {

		// Check if the record exists
		PreparedStatement preparedStatement1 = null;
		PreparedStatement preparedStatement2 = null;
		PreparedStatement preparedStatement3 = null;
		ResultSet resultSet = null;
		try {
			preparedStatement1 = connection.prepareStatement("select CardID,ColumnName,"
					+ "Backlog,Planned,USReady,USReviewed,DesignReviewed,InProgress,Blocked,CodeReviewed,SamplesDone,TestsAutomated,Done"
					+ " from Cards where CardID = ?");
			preparedStatement1.setString(1, gitCard.getCardId());
			resultSet = preparedStatement1.executeQuery();

			// If the record exists update
			if (resultSet.next()) {

				String currentColumn = resultSet.getString(2);
				boolean columnChange = false;
				if (!currentColumn.equals(gitCard.getColumn())) {
					columnChange = true;
				}

				//Update the exising card record
				StringBuilder sb1 = new StringBuilder();				
				sb1.append("update Cards set ");
				sb1.append("ProjectId = ?,");
				sb1.append("ColumnName = ?,");
				sb1.append("Note = ?,");
				sb1.append("IssueId = ?,");
				sb1.append("Title = ?,");
				sb1.append("AssigneeId = ?,");
				sb1.append("Lastmodifiedtimestamp = ?,");
				sb1.append("IssueLink = ?");				
				if (columnChange) {
					sb1.append("," + getNewStateColumn(gitCard.getColumn()) + " = ?");
				}
				sb1.append(" where CardID = ?");
				preparedStatement2 = connection.prepareStatement(sb1.toString());
				preparedStatement2.setString(1, gitCard.getProjectId());
				preparedStatement2.setString(2, gitCard.getColumn());
				preparedStatement2.setString(3, gitCard.getNote());
				preparedStatement2.setString(4, gitCard.getIssueId());
				preparedStatement2.setString(5, gitCard.getTitle());
				preparedStatement2.setString(6, gitCard.getAssigneeId());
				preparedStatement2.setTimestamp(7, new Timestamp(currentTimestamp));
				preparedStatement2.setString(8, gitCard.getIssueLink());
				if (columnChange) {
					preparedStatement2.setTimestamp(9, new Timestamp(new Date().getTime()));
					preparedStatement2.setString(10, gitCard.getCardId());

					// Add record to history table
					preparedStatement3 = connection.prepareStatement(
							"insert into CardHistory (CardID,ColumnName,FromTimestamp,ToTimestamp,Lastmodifiedtimestamp) values (?,?,?,?,?)");
					preparedStatement3.setString(1, gitCard.getCardId());
					preparedStatement3.setString(2, currentColumn);
					Timestamp tFromTimestamp = getFromTimestamp(currentColumn, resultSet);
					if (tFromTimestamp != null) {
						preparedStatement3.setTimestamp(3, tFromTimestamp);
					} else {
						preparedStatement3.setTimestamp(3, new Timestamp(new Date().getTime()));
					}
					preparedStatement3.setTimestamp(4, new Timestamp(new Date().getTime()));
					preparedStatement3.setTimestamp(5, new Timestamp(currentTimestamp));
					preparedStatement3.executeUpdate();
				} else {
					preparedStatement2.setString(9, gitCard.getCardId());
				}
				
				return preparedStatement2.executeUpdate();

			} else {
				StringBuilder sb1 = new StringBuilder();
				StringBuilder sb2 = new StringBuilder();
				sb1.append("insert into Cards ");
				sb1.append("(CardID,ProjectId,ColumnName,Note,IssueId,"
						+ "Title,AssigneeId,Lastmodifiedtimestamp,IssueLink,");
				sb1.append(getNewStateColumn(gitCard.getColumn()));
				sb1.append(") values (?,?,?,?,?,?,?,?,?,?)");

				preparedStatement2 = connection.prepareStatement(sb1.toString());

				preparedStatement2.setString(1, gitCard.getCardId());
				preparedStatement2.setString(2, gitCard.getProjectId());
				preparedStatement2.setString(3, gitCard.getColumn());
				preparedStatement2.setString(4, gitCard.getNote());
				preparedStatement2.setString(5, gitCard.getIssueId());
				preparedStatement2.setString(6, gitCard.getTitle());
				preparedStatement2.setString(7, gitCard.getAssigneeId());
				preparedStatement2.setTimestamp(8, new Timestamp(currentTimestamp));
				preparedStatement2.setString(9, gitCard.getIssueLink());
				preparedStatement2.setTimestamp(10, new Timestamp(new Date().getTime()));
				return preparedStatement2.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				preparedStatement1.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				preparedStatement2.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (preparedStatement3 != null) {
				try {
					preparedStatement3.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param strColumn
	 * @return
	 */
	private String getNewStateColumn(String strColumn) {
		switch (strColumn) {
		case "Backlog":
			return "Backlog";
		case "Planned":
			return "Planned";
		case "User Stories (Ready)":
			return "USReady";
		case "User Stories (Reviewed)":
			return "USReviewed";
		case "Design Reviewed":
			return "DesignReviewed";
		case "In Progress":
			return "InProgress";
		case "Blocked":
			return "Blocked";
		case "Code Reviewed":
			return "CodeReviewed";
		case "Samples Done":
			return "SamplesDone";
		case "Tests Automated":
			return "TestsAutomated";
		case "Done":
			return "Done";
		default:
			return "Backlog";
		}

	}

	/**
	 * 
	 * Get the timestamp when moved to the current column
	 * 
	 * @param strColumn
	 * @return
	 */
	private Timestamp getFromTimestamp(String sCurrentColumn, ResultSet rs) {
		// Following order from index 3
		// Backlog,Planned,USReady,USReviewed,DesignReviewed,InProgress,Blocked,CodeReviewed,SamplesDone,TestsAutomated,Done
		try {
			switch (sCurrentColumn) {
			case "Backlog":
				return rs.getTimestamp(3);
			case "Planned":
				return rs.getTimestamp(4);
			case "User Stories (Ready)":
				return rs.getTimestamp(5);
			case "User Stories (Reviewed)":
				return rs.getTimestamp(6);
			case "Design Reviewed":
				return rs.getTimestamp(7);
			case "In Progress":
				return rs.getTimestamp(8);
			case "Blocked":
				return rs.getTimestamp(9);
			case "Code Reviewed":
				return rs.getTimestamp(10);
			case "Samples Done":
				return rs.getTimestamp(11);
			case "Tests Automated":
				return rs.getTimestamp(12);
			case "Done":
				return rs.getTimestamp(13);
			default:
				return rs.getTimestamp(3);
			}
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * Delete the records that were not update in the cycle
	 * 
	 * @param strTableNames
	 */
	public void cleanRecords(String...strTableNames){
		for(String strTableName:strTableNames){			
			try (PreparedStatement preparedStatement = connection.prepareStatement("delete from " + strTableName + " where Lastmodifiedtimestamp < ?")) {
				preparedStatement.setTimestamp(1, new Timestamp(currentTimestamp));
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();				
			}
		}
	}
	
	/**
	 * 
	 * If a record exists update. Otherwise insert.
	 * 
	 * @param id
	 * @param tableName
	 * @param mColumns
	 * @return (0 - no effect, 1 - inserted, 2 - Updated)
	 */
	public int upsertRecord(String id, String tableName, Map<String, String> mColumns) {

		// Check if the record exists
		PreparedStatement preparedStatement1 = null;
		PreparedStatement preparedStatement2 = null;
		ResultSet resultSet = null;
		String strSql = "select " + id + " from " + tableName + " where " + id + " = ?";
		try {
			preparedStatement1 = connection.prepareStatement(strSql);
			preparedStatement1.setString(1, mColumns.get(id));
			resultSet = preparedStatement1.executeQuery();

			// If the record exists update
			if (resultSet.next()) {
				StringBuilder sb = new StringBuilder();
				sb.append("update " + tableName);
				boolean firstElement = true;
				for (String strkey : mColumns.keySet()) {
					if (!strkey.equals(id)) {
						if (firstElement) {
							sb.append(" set " + strkey + " = ?");
							firstElement = false;
						} else {
							sb.append("," + strkey + " = ?");
						}

					}
				}
				sb.append(",Lastmodifiedtimestamp = ?");
				sb.append(" where " + id + " = ?");
				preparedStatement2 = connection.prepareStatement(sb.toString());
				int i = 0;
				for (String strkey : mColumns.keySet()) {
					if (!strkey.equals(id)) {
						preparedStatement2.setString(++i, mColumns.get(strkey));
					}
				}
				preparedStatement2.setTimestamp(++i, new Timestamp(currentTimestamp));
				preparedStatement2.setString(++i, mColumns.get(id));
				return preparedStatement2.executeUpdate();

			} else {
				StringBuilder sb1 = new StringBuilder();
				StringBuilder sb2 = new StringBuilder();
				boolean firstColumn = true;
				sb1.append("insert into " + tableName + " (");
				for (String strkey : mColumns.keySet()) {
					if (firstColumn) {
						sb1.append(strkey);
						sb2.append("?");
						firstColumn = false;
					} else {
						sb1.append("," + strkey);
						sb2.append(",?");
					}
				}
				preparedStatement2 = connection.prepareStatement(sb1.toString() + ",Lastmodifiedtimestamp) values (" + sb2.toString() + ",?)");
				int i = 0;
				for (String strkey : mColumns.keySet()) {
					preparedStatement2.setString(++i, mColumns.get(strkey));
				}
				preparedStatement2.setTimestamp(++i, new Timestamp(currentTimestamp));
				return preparedStatement2.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				preparedStatement1.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				preparedStatement2.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Close the open connection made with the constructor
	 */
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
