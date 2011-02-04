//The sample client application begins by importing the necessary packages and objects.  

package com.doc.samples;

import java.io.*;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.ExceptionCode;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.sobject.Contact;

/**
 * Title: Login Sample
 * 
 * Description: Console application illustrating login, session management,
 * and server redirection.
 * 
 * Copyright: Copyright (c) 2005- 2008
 * Company: salesforce.com
 *
 * @version 14.0
 */  
    
public class Samples {
    private SoapBindingStub binding;
    static BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));

    public Samples() {
    }

    public static void main(String[] args) throws ServiceException {
        Samples samples1 = new Samples();
        samples1.run();
    }

    //The sample client application retrieves the user's login credentials.  
    
    // Helper function for retrieving user input from the console  
    
    String getUserInput(String prompt) {
        System.out.print(prompt);
        try {
            return rdr.readLine();
        }
        catch (IOException ex) {
            return null;
        }
    }

    /**
     * The login call is used to obtain a token from Salesforce.
     * This token must be passed to all other calls to provide
     * authentication.
     */  
    
    private boolean login() throws ServiceException {
        String userName = getUserInput("Enter username: ");
        String password = getUserInput("Enter password: ");
        /** Next, the sample client application initializes the binding stub.
         * This is our main interface to the API through which all 
         * calls are made. The getSoap method takes an optional parameter,
         * (a java.net.URL) which is the endpoint.
         * For the login call, the parameter always starts with 
         * http(s)://login.salesforce.com. After logging in, the sample 
         * client application changes the endpoint to the one specified 
         * in the returned loginResult object.
         */  
    
        binding = (SoapBindingStub) new SforceServiceLocator().getSoap();
        
        // Time out after a minute  
    
        binding.setTimeout(60000);
        // Test operation  
    
        LoginResult loginResult;
        try {
            System.out.println("LOGGING IN NOW....");
            loginResult = binding.login(userName, password);
        }
        catch (LoginFault ex) {
            // The LoginFault derives from AxisFault  
    
            ExceptionCode exCode = ex.getExceptionCode();
            if (exCode == ExceptionCode.FUNCTIONALITY_NOT_ENABLED ||
                exCode == ExceptionCode.INVALID_CLIENT ||
                exCode == ExceptionCode.INVALID_LOGIN ||
                exCode == ExceptionCode.LOGIN_DURING_RESTRICTED_DOMAIN ||
                exCode == ExceptionCode.LOGIN_DURING_RESTRICTED_TIME ||
                exCode == ExceptionCode.ORG_LOCKED ||
                exCode == ExceptionCode.PASSWORD_LOCKOUT ||
                exCode == ExceptionCode.SERVER_UNAVAILABLE ||
                exCode == ExceptionCode.TRIAL_EXPIRED ||
                exCode == ExceptionCode.UNSUPPORTED_CLIENT) {
                System.out.println("Please be sure that you have a valid username " +
                     "and password.");
            } else {
                // Write the fault code to the console  
    
                System.out.println(ex.getExceptionCode());
                // Write the fault message to the console  
    
                System.out.println("An unexpected error has occurred." + ex.getMessage());
            }
            return false;
        } catch (Exception ex) {
            System.out.println("An unexpected error has occurred: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        // Check if the password has expired  
    
        if (loginResult.isPasswordExpired()) {
            System.out.println("An error has occurred. Your password has expired.");
            return false;
        }
        /** Once the client application has logged in successfully, it will use 
         *  the results of the login call to reset the endpoint of the service  
         *  to the virtual server instance that is servicing your organization.  
         *  To do this, the client application sets the ENDPOINT_ADDRESS_PROPERTY 
         *  of the binding object using the URL returned from the LoginResult.
         */  
    
        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, 
            loginResult.getServerUrl());
        /** The sample client application now has an instance of the SoapBindingStub 
         *  that is pointing to the correct endpoint. Next, the sample client application 
         *  sets a persistent SOAP header (to be included on all subsequent calls that 
         *  are made with the SoapBindingStub) that contains the valid sessionId
         *  for our login credentials. To do this, the sample client application 
         *  creates a new SessionHeader object and set its sessionId property to the 
         *  sessionId property from the LoginResult object.
         */  
    
        // Create a new session header object and add the session id  
    
        // from the login return object  
    
        SessionHeader sh = new SessionHeader();
        sh.setSessionId(loginResult.getSessionId());
        /** Next, the sample client application calls the setHeader method of the 
         *  SoapBindingStub to add the header to all subsequent method calls. This  
         *  header will persist until the SoapBindingStub is destroyed until the header 
         *  is explicitly removed. The "SessionHeader" parameter is the name of the 
         *  header to be added.
         */  
    
        // set the session header for subsequent call authentication  
    
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(),
                          "SessionHeader", sh);
        // return true to indicate that we are logged in, pointed  
    
        // at the right url and have our security token in place.  
    
        return true;
    }

    /**
     * To determine the objects that are available to the logged-in user, the sample 
     * client application executes a describeGlobal call, which returns all of the 
     * objects that are visible to the logged-in user. This call should not be made  
     * more than once per session, as the data returned from the call likely does not  
     * change frequently. The DescribeGlobalResult is simply echoed to the console.
     */  
    
    private void describeGlobalSample() {
        try
        {
            DescribeGlobalResult describeGlobalResult = null;
            describeGlobalResult = binding.describeGlobal();
            DescribeGlobalSObjectResult[] sobjectResults
              = describeGlobalResult.getSobjects();
            for (int i=0;i<sobjectResults.length;i++) {
                System.out.println(sobjectResults[i].getName());
            }
        }
        catch (Exception ex)
        {
            System.out.println("\nFailed to return types, error message was: \n" + 
                ex.getMessage());
        }
    }

    /**
     * The following code segment illustrates the type of metadata information that 
     * can be obtained for each object available to the user. The sample client 
     * application executes a describeSObject call on a given object and then echoes  
     * the returned metadata information to the console. Object metadata information
     * includes permissions, field types and length and available values for picklist 
     * fields and types for referenceTo fields.
     */  
    
    private void describeSample() {
        String objectToDescribe = getUserInput("\nType the name of the object to " +
             "describe (try Account): ");
        try {
            DescribeSObjectResult descSObjectRslt;
            descSObjectRslt = binding.describeSObject(objectToDescribe);
            if (descSObjectRslt != null) {
                // Report object level information  
    
                Field[] fields = descSObjectRslt.getFields();
                String objectName = descSObjectRslt.getName();
                System.out.println("Metadata for " + objectToDescribe + " object:\n");
                System.out.println("Object name = " + objectName);
                System.out.println("Number of fields = " + fields.length);
                System.out.println("Object can be activated = " + 
                     descSObjectRslt.isActivateable());
                System.out.println("Can create rows of data = " + 
                     descSObjectRslt.isCreateable());
                System.out.println("Object is custom object = " + 
                     descSObjectRslt.isCustom());
                System.out.println("Can delete rows of data = " + 
                     descSObjectRslt.isDeletable());
                System.out.println("Can query for rows of data = " + 
                     descSObjectRslt.isQueryable());
                System.out.println("Object used in replication = " + 
                     descSObjectRslt.isReplicateable());
                System.out.println("Can retrieve object = " + 
                     descSObjectRslt.isRetrieveable());
                System.out.println("Can search object = " + 
                     descSObjectRslt.isSearchable());
                System.out.println("Can un-delete = " + 
                     descSObjectRslt.isUndeletable());
                System.out.println("Can update = " + 
                     descSObjectRslt.isUpdateable());
                System.out.println("\nField metadata for " + objectToDescribe + 
                     " object:\n");
                // Report information about each field  
    
                if (fields != null) {
                    for (Field field : fields) {
                        PicklistEntry[] picklistValues = field.getPicklistValues();
                        String[] referenceTos = field.getReferenceTo();
                        System.out.println("************* New Field ***************");
                        System.out.println("Name = " + field.getName());
                        System.out.println("Label = " + field.getLabel());
                        System.out.println("Length = " + field.getLength());
                        System.out.println("Bytelength = " + field.getByteLength());
                        System.out.println("Digits = " + field.getDigits());
                        System.out.println("Precision = " + field.getPrecision());
                        System.out.println("Scale = " + field.getScale());
                        System.out.println("Field type = " + field.getType());
                        // field properties  
    
                        System.out.println("Custom field = " + field.isCustom());
                        System.out.println("Name field = " + field.isNameField());
                        System.out.println("Can set field value on Create = " + 
                             field.isCreateable());
                        System.out.println("Can set field value on Update = " + 
                             field.isUpdateable());
                        System.out.println("Can be used to filter results = " + 
                             field.isFilterable());
                        System.out.println("Field value can be empty = " + 
                             field.isNillable());
                        System.out.println("Field value is defaulted on Create = " + 
                             field.isDefaultedOnCreate());
                        System.out.println("Field value is calculated = " + 
                             field.isCalculated());
                        System.out.println("Field value is a restricted picklist = " + 
                             field.isRestrictedPicklist());
                        if (picklistValues != null) {
                            System.out.println("Picklist values = ");
                            for (PicklistEntry picklistValue : picklistValues) {
                                if (picklistValue.getLabel() != null)
                                    System.out.print(" item: " + picklistValue.getLabel());
                                else
                                    System.out.print(" item: " + picklistValue.getValue());
                                System.out.print(", value = " + picklistValue.getValue());
                                System.out.println(", is default = " + 
                                     picklistValue.isDefaultValue());
                            }
                        }
                        if (referenceTos != null) {
                            System.out.println("Field references the following objects:");
                            for (String referenceTo : referenceTos) System.out.println(" " 
                                + referenceTo);
                        }
                        System.out.println("");
                    }
                    getUserInput("\nDescribe " + objectToDescribe +
                         " was successful.\n\nHit the enter key to continue....");
                }
            }
        } catch (Exception ex) {
            System.out.println("\nFailed to get " + objectToDescribe +
                 " description, error message was: \n " + ex.getMessage());
            getUserInput("\nHit return to continue...");
        }
    }

    /**
     * The sample client application executes a query by invoking the query call, 
     * passing a simple query string ("select FirstName, LastName from Contact") 
     * and iterating through the returned QueryResult.
     */  
    
    private void querySample() {
        QueryOptions qo = new QueryOptions();
        qo.setBatchSize(200);
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), 
             "QueryOptions", qo);
        try {
            QueryResult qr = binding.query("select FirstName, LastName from Contact");
            
            if (qr.getSize() > 0) {
                      System.out.println("Logged in user can see "
                          + qr.getRecords().length + " contact records. ");
                      do {
                          // output contact records  
    
                          for (int i = 0; i < qr.getRecords().length; i++) {
                              Contact con = (Contact) qr.getRecords(i);
                              String fName = con.getFirstName();
                              String lName = con.getLastName();
                              if (fName == null) {
                                  System.out.println("Contact " + (i + 1) + ": "
                                   + lName);
                              } else {
                                      System.out.println("Contact " + (i + 1) + ": "
                                       + fName + " " + lName);
                              }
                          }

                       if (!qr.isDone()) {
                               qr = binding.queryMore(qr.getQueryLocator());
                       } else {
                               break;
                       }
                   } while (qr.getSize() > 0);
           } else {
                    System.out.println("No records found.");
           }            

          getUserInput("Query succesfully executed. \nHit return to continue...");
     } catch (RemoteException ex) {
         System.out.println("\nFailed to execute query succesfully, error message was:" + 
                            "\n" + ex.getMessage());
         getUserInput("\nHit return to continue...");
     }
    }

    private void run() throws ServiceException {
        if (login()) {
            getUserInput("SUCCESSFUL LOGIN! Hit the enter key to continue.");
            describeGlobalSample();
            describeSample();
            querySample();
        }
    }
}