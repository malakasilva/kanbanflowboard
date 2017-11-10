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

/**
 * 
 * This is the invoker class that will trigger the task.
 * This cass should be called on regular interval by another process
 *
 */

public class Main {

  public static void main(String[] args) {
    System.out.println("Starting the Git to DB Sync Service");
    ReadData readData = new ReadData();
    readData.readWriteData();
    System.out.println("Ended the Git to DB Sync Service");
  }
  
}
