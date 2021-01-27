package turtleisaac;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsAPI
{
    private Sheets sheetsService;
    private String APPLICATION_NAME= "PokEditor";
    private String SPREADSHEET_ID;
    private String SPREADSHEET_LINK;
    private String projectPath;

    public GoogleSheetsAPI(String spreadsheetLink, String projectPath) throws IOException, GeneralSecurityException
    {
        if(!spreadsheetLink.contains("https://"))
            spreadsheetLink= "https://" + spreadsheetLink;

        SPREADSHEET_LINK= spreadsheetLink;
        this.projectPath= projectPath;

        SPREADSHEET_ID= spreadsheetLink.split("/")[5];
        sheetsService= getSheetsService();


    }

    private Credential authorize() throws IOException, GeneralSecurityException
    {
        InputStream in= GoogleSheetsAPI.class.getResourceAsStream("/credentials.json");

        GoogleClientSecrets clientSecrets= GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));

        List<String> scopes= Arrays.asList(SheetsScopes.SPREADSHEETS);
        GoogleAuthorizationCodeFlow flow= new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(),JacksonFactory.getDefaultInstance(),clientSecrets,scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new File(projectPath + "/tokens")))
                .setAccessType("offline")
                .build();

        Credential credential= new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        return credential;
    }

    public Sheets getSheetsService() throws IOException, GeneralSecurityException
    {
        Credential credential= authorize();
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<List<Object>> getResponse(String subSheet, String range) throws IOException
    {
        ValueRange response= sheetsService.spreadsheets().values().get(SPREADSHEET_ID,subSheet + "!" + range).execute();

        List<List<Object>> values= response.getValues();

        return values;
    }

    public List<List<Object>> getSpecifiedSheet(String subSheet) throws IOException
    {
        ValueRange response= sheetsService.spreadsheets().values().get(SPREADSHEET_ID,subSheet).execute();

        List<List<Object>> values= response.getValues();

        System.out.println("Downloading sheet: " + subSheet + " (" + values.size() + " rows and " + values.get(0).size() + " columns)\n");

        return values;
    }

    public Object[][] getSpecifiedSheetArr(String subSheet) throws IOException
    {
        List<List<Object>> values= getSpecifiedSheet(subSheet);

        Object[][] ret= new Object[values.size()][];

        for(int i= 0; i < values.size(); i++)
            ret[i]= values.get(i).toArray(new Object[0]);

        return ret;
    }

    public void appendSheet(String range) throws IOException
    {
        ValueRange appendBody= new ValueRange()
                .setValues(Arrays.asList(Arrays.asList("This","was","added","from","PokEditor!")));

        AppendValuesResponse appendResult= sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID,range,appendBody)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .setIncludeValuesInResponse(false)
                .execute();
    }

    public void updateSheet(String range, List<List<Object>> values) throws IOException
    {
        ValueRange body= new ValueRange()
                .setValues(values);

        UpdateValuesResponse result= sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID,range,body)
                .setValueInputOption("USER_ENTERED")
                .setIncludeValuesInResponse(false)
                .execute();
    }

    public void updateSheet(String range, Object[][] values) throws IOException
    {
        List<List<Object>> data= new ArrayList<>();

        for(Object[] arr : values)
        {
            System.out.println(Arrays.toString(arr));
            List<Object> row = new ArrayList<>(Arrays.asList(arr));
            data.add(row);
        }

        updateSheet(range, data);
    }

    public void updateSheet(String range, Object[] values) throws IOException
    {
        List<Object> row = new ArrayList<>(Arrays.asList(values));
        List<List<Object>> data= new ArrayList<>();
        data.add(row);
        updateSheet(range, data);
    }

    public void deleteRow() throws IOException
    {
        DeleteDimensionRequest deleteRequest= new DeleteDimensionRequest()
                .setRange(
                        new DimensionRange()
                                .setSheetId(1880122989)
                                .setDimension("ROWS")
                                .setStartIndex(20)
                );

        List<Request> requests= new ArrayList<>();
        requests.add(new Request().setDeleteDimension(deleteRequest));

        BatchUpdateSpreadsheetRequest body= new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID,body).execute();

        System.out.println("Delete successful (hopefully)");
    }

    public String[] getSheetNames() throws IOException
    {
        Spreadsheet response1= sheetsService.spreadsheets().get(SPREADSHEET_ID)
                .setIncludeGridData(false)
                .execute();

        List<Sheet> sheetList = response1.getSheets();

        List<String> sheetNames= new ArrayList<>();

        System.out.println("\n----Sheet Names----");
        for (Sheet sheet : sheetList)
        {
            System.out.println(sheet.getProperties().getTitle());
            sheetNames.add(sheet.getProperties().getTitle());
        }
        System.out.println("-------------------\n");

        return sheetNames.toArray(new String[0]);
    }



    public String getPokeditorSheetType(int sheetId) throws IOException
    {
        return sheetsService.spreadsheets().get(SPREADSHEET_ID)
                .setIncludeGridData(true)
                .setFields("sheets/data/rowData/values/note")
                .execute()
                .getSheets()
                .get(sheetId)
                .getData()
                .get(0)
                .getRowData()
                .get(0)
                .getValues()
                .get(0)
                .getNote();
    }

    public void setPokeditorSheetType(String sheetName, String note)
    {
        try
        {
            List<Sheet> sheetList= sheetsService.spreadsheets().get(SPREADSHEET_ID).execute().getSheets();

            int sheetIndex= indexOf(sheetList,sheetName);
            Integer sheetId= sheetList.get(sheetIndex).getProperties().getSheetId();
            System.out.println("Sheet ID: " + sheetId);

            List<Request> requests= new ArrayList<>();
            requests.add(buildRequestAddNoteCell(0,0,0,0,sheetId,note));

            BatchUpdateSpreadsheetRequest body= new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID,body).execute();
        }
        catch (IOException | NullPointerException exception)
        {
            exception.printStackTrace();
        }

//        System.out.println(sheetsService.spreadsheets().developerMetadata().);

    }

    private Request buildRequestAddNoteCell(int startRow, int endRow, int startColumn, int endColumn, int sheetId, String note) {
        Request request = new Request();
        request.setRepeatCell(new RepeatCellRequest()
                .setCell(new CellData().setNote(note))
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(startRow)
                        .setEndRowIndex(endRow)
                        .setStartColumnIndex(startColumn)
                        .setEndColumnIndex(endColumn))
                .setFields("note")
        );
        return request;
    }

    public void pokeditorSheetMetadataUpdater(String[] expectedNames) throws IOException
    {
        List<Sheet> sheetList= sheetsService.spreadsheets().get(SPREADSHEET_ID).execute().getSheets();
        List<Request> requests= new ArrayList<>();
        String[] sheetNames= getSheetNames();
        boolean dataStart= false;

        for(int i= 0; i < sheetNames.length; i++)
        {
            String sheetName= sheetNames[i];
            if(sheetName.equals("Personal"))
                dataStart= true;
            if(sheetName.equals("Formatting (DO NOT TOUCH)"))
                break;

            Integer sheetId= sheetList.get(i).getProperties().getSheetId();

            if(dataStart)
                requests.add(buildRequestAddNoteCell(0,1,0,1,sheetId,expectedNames[i]));
        }

        BatchUpdateSpreadsheetRequest body= new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID,body).execute();
    }

//    public void getMetadata(int id) throws IOException
//    {
//        System.out.println(sheetsService.spreadsheets().developerMetadata().get(SPREADSHEET_ID,id).get("Personal"));
//    }

    public void pokeditorSheetAutoUpdater()
    {

    }

    public String getAPPLICATION_NAME()
    {
        return APPLICATION_NAME;
    }

    public String getSPREADSHEET_ID()
    {
        return SPREADSHEET_ID;
    }

    public String getSPREADSHEET_LINK()
    {
        return SPREADSHEET_LINK;
    }

    public void setAPPLICATION_NAME(String APPLICATION_NAME)
    {
        this.APPLICATION_NAME = APPLICATION_NAME;
    }

//    public static void main(String[] args) throws IOException, GeneralSecurityException
//    {
//        GoogleSheetsAPI googleSheets= new GoogleSheetsAPI("https://docs.google.com/spreadsheets/d/1XxVz-Y5kpStjsMtgrtALD7DMh1OAvbYsOJl5WtDPi2k/edit#gid=414420977");
//        String subSheet= "Personal";
//        String range= "A2:H20";
//
//        List<List<Object>> values= googleSheets.getResponse(subSheet, range);
//
//        if(values == null || values.isEmpty())
//        {
//            System.out.println("No data found.");
//        }
//        else
//        {
//            for(List row : values)
//            {
//                System.out.printf("Species %s with ID %s\n",row.get(1),row.get(0));
//            }
//        }
//
//
//
//    }

    private int indexOf(String[] arr, String str)
    {
        for(int i= 0; i < arr.length; i++)
        {
            if(arr[i].equals(str))
                return i;
        }
        return -1;
    }

    private int indexOf(List<Sheet> list, String str)
    {
        for(int i= 0; i < list.size(); i++)
        {
            if(list.get(i).getProperties().getTitle().equals(str))
                return i;
        }
        return -1;
    }
}
