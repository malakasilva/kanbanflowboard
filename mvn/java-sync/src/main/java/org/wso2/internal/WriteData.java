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
		ResultSet resultSet = null;
		try {
			preparedStatement1 = connection.prepareStatement("select CardID,ColumnName from Cards where CardID = ?");
			preparedStatement1.setString(1, gitCard.getCardId());
			resultSet = preparedStatement1.executeQuery();

			// If the record exists update
			if (resultSet.next()) {

				String currentColumn = resultSet.getString(2);
				boolean columnChange = false;
				if (!currentColumn.equals(gitCard.getColumn())) {
					columnChange = true;
				}

				StringBuilder sb = new StringBuilder();
				sb.append("update Cards set ");
				sb.append("ProjectId = ?,");
				sb.append("ColumnName = ?,");
				sb.append("Note = ?,");
				sb.append("IssueId = ?,");
				sb.append("Title = ?,");
				sb.append("AssigneeId = ? ");
				if (columnChange) {
					sb.append("," + getNewStateColumn(gitCard.getColumn()) + " = ? ");
				}
				sb.append("where CardID = ?");
				preparedStatement2 = connection.prepareStatement(sb.toString());
				preparedStatement2.setString(1, gitCard.getProjectId());
				preparedStatement2.setString(2, gitCard.getColumn());
				preparedStatement2.setString(3, gitCard.getNote());
				preparedStatement2.setString(4, gitCard.getIssueId());
				preparedStatement2.setString(5, gitCard.getTitle());
				preparedStatement2.setString(6, gitCard.getAssigneeId());
				if (columnChange) {
					preparedStatement2.setTimestamp(7, new Timestamp(new Date().getTime()));
					preparedStatement2.setString(8, gitCard.getCardId());
				} else {
					preparedStatement2.setString(7, gitCard.getCardId());
				}

				return preparedStatement2.executeUpdate();

			} else {
				StringBuilder sb1 = new StringBuilder();
				StringBuilder sb2 = new StringBuilder();
				sb1.append("insert into Cards ");
				sb1.append("(CardID,ProjectId,ColumnName,Note,IssueId,Title,AssigneeId,");
				sb1.append(getNewStateColumn(gitCard.getColumn()));
				sb1.append(") values (?,?,?,?,?,?,?,?)");

				preparedStatement2 = connection.prepareStatement(sb1.toString());

				preparedStatement2.setString(1, gitCard.getCardId());
				preparedStatement2.setString(2, gitCard.getProjectId());
				preparedStatement2.setString(3, gitCard.getColumn());
				preparedStatement2.setString(4, gitCard.getNote());
				preparedStatement2.setString(5, gitCard.getIssueId());
				preparedStatement2.setString(6, gitCard.getTitle());
				preparedStatement2.setString(7, gitCard.getAssigneeId());
				preparedStatement2.setTimestamp(8, new Timestamp(new Date().getTime()));
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
				sb.append(" where " + id + " = ?");
				preparedStatement2 = connection.prepareStatement(sb.toString());
				int i = 0;
				for (String strkey : mColumns.keySet()) {
					if (!strkey.equals(id)) {
						preparedStatement2.setString(++i, mColumns.get(strkey));
					}
				}
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
				preparedStatement2 = connection.prepareStatement(sb1.toString() + ") values (" + sb2.toString() + ")");
				int i = 0;
				for (String strkey : mColumns.keySet()) {
					preparedStatement2.setString(++i, mColumns.get(strkey));
				}
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