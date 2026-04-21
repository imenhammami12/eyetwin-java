package com.eyetwin;

import com.eyetwin.tools.DatabaseConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbSchemaReader {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("SCHEMA_START");
            String[] tables = {"tournoi", "matches", "match", "matchs"};
            for (String tableName : tables) {
                System.out.println("---- Table: " + tableName + " ----");
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                boolean found = false;
                while (columns.next()) {
                    found = true;
                    String columnName = columns.getString("COLUMN_NAME");
                    String datatype = columns.getString("TYPE_NAME");
                    String columnsize = columns.getString("COLUMN_SIZE");
                    System.out.println(columnName + " - " + datatype + " - " + columnsize);
                }
                if (!found) System.out.println("Not found.");
            }
            System.out.println("SCHEMA_END");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
