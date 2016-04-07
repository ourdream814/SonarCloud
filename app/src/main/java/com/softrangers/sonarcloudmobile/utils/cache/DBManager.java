package com.softrangers.sonarcloudmobile.utils.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by eduard on 4/4/16.
 */
public class DBManager {
    private static DBManager mInstance;
    private SQLiteOpenHelper mOpenHelper;

    private DBManager(Context context) {
        mOpenHelper = new SQLiteOpenHelper(context, Constants.DB_CACHE_DIR + Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                try {
                    sqLiteDatabase.execSQL(Constants.CREATE_RESPONSE_TABLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
                try {
                    sqLiteDatabase.execSQL(Constants.DROP_TABLE_RESPONSE);
                    onCreate(sqLiteDatabase);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        getDataBase();
    }

    public static synchronized DBManager getInstance() {
        if (mInstance == null)
            mInstance = new DBManager(SonarCloudApp.getInstance().getApplicationContext());
        return mInstance;
    }

    /**
     * Return an instance of writable database
     */
    public SQLiteDatabase getDataBase() {
        return mOpenHelper.getWritableDatabase();
    }


    /**
     * Записывает данные в таблицу
     *
     * @param table   Имя таблицы
     * @param values  Данные для добавления
     * @param columns В какие столбцы добавить данные (должны быть в том порядке что и values)
     */
    public void insertInTable(String table, String[] values, String[] columns) {
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (values.length == 1) {
                valueBuilder.append(values[i]);
            } else if (i == 0) {
                valueBuilder.append("");
                valueBuilder.append(values[i]);
                valueBuilder.append("'");
            } else if (i == values.length - 1) {
                valueBuilder.append(", '");
                valueBuilder.append(values[i]);
            } else {
                valueBuilder.append(", '");
                valueBuilder.append(values[i]);
                valueBuilder.append("'");
            }
        }
        String strValues = valueBuilder.toString();

        StringBuilder columnBuilder = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i == 0) {
                columnBuilder.append("");
                columnBuilder.append(columns[i]);
            } else {
                columnBuilder.append(", ");
                columnBuilder.append(columns[i]);
            }
        }
        String strColumns = columnBuilder.toString();

        String insert = "INSERT OR REPLACE INTO " + table + " (" + strColumns + ") VALUES ('" + strValues + "');";
        ////Log.e(TAG, insert);
        try {
            getDataBase().execSQL(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Запрос на возвращение столбцов с нужными строками из таблицы
     *
     * @param table   Имя таблицы
     * @param columns Масив строк с именами нужных столбцов
     * @return Возвращает Cursor
     */
    public Cursor queryColumns(String table, String... columns) {
        return getDataBase().query(table, columns, null, null, null, null, null);
    }

    public Cursor queryColumns(String table, String[] column, String requestColumn, String where) {
        return getDataBase().query(table, column, requestColumn + "='" + where + "'", null, null, null, null);
    }


    /**
     * Запрос на возвращение строк из таблицы
     *
     * @param sql           SQL запрос. Щн не должен заканчиваться с ;
     * @param selectionArgs Можете включить ?s в WHERE и он будет заменятся на selectionArgs.
     * @return Возвращает строки из таблицы
     */
    public Cursor queryRows(SQLiteDatabase database, String sql, String[] selectionArgs) {
        return database.rawQuery(sql, selectionArgs);
    }


    /**
     * Used to delete all data from one table
     * @param table table you want to delete data from
     */
    public void deleteAllData(String table) {
        String delete = "DELETE FROM " + table;
        getDataBase().execSQL(delete);
    }


    /**
     * Used to delete one row from table
     * @param table table you want to delete from
     * @param column column from where to search the requested row
     * @param where data within the row that you want to delete
     */
    public void deleteRow(String table, String column, String... where) {
        String delete = "DELETE FROM " + table + " WHERE " + column + " = " + Arrays.toString(where);
        getDataBase().execSQL(delete);
    }



    /**
     * Search the data in database using the given key
     * @param request the request url is used like primary key
     * @return a last inserted JSON with the given key (request URL)
     */
    public static String loadDataFromDB(JSONObject request) {
        request.remove("seq");
        String[] columns = {Constants.RESPONSE};
        Cursor cursor = DBManager.getInstance().queryColumns(Constants.RESPONSE_TABLE, columns,
                Constants.REQUEST, request.toString());
        String response = "";
        try {
            while (cursor.moveToNext()) {
                int responseIndex = cursor.getColumnIndex(Constants.RESPONSE);
                response = cursor.getString(responseIndex);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }


    /**
     * Delete all the data from the table except users login data (password and id)
     * @param table table you want to delete from
     */
    public void deleteDB(String table) {

    }
}
