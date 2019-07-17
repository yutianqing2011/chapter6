package com.byted.camp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.debug.DebugActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    private TodoDbHelper mDbHelper;
    private SQLiteDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        mDbHelper=new TodoDbHelper(this);
        db=mDbHelper.getWritableDatabase();

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                MainActivity.this.deleteNote(note);
            }

            @Override
            public void updateNote(Note note) {
                MainActivity.this.updateNode(note);
            }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        // TODO 从数据库中查询数据，并转换成 JavaBeans
        List<Note> result=new LinkedList<>();
        Cursor cursor=null;


        if(db==null){
            return Collections.emptyList();
        }
        String[] projection={
                BaseColumns._ID,
                TodoContract.TodoEntry.COLUMN_NAME_DATA,
                TodoContract.TodoEntry.COLUMN_NAME_STATE,
                TodoContract.TodoEntry.COLUMN_NAME_CONTENT,
                //TodoContract.TodoEntry.COLUMN_NAME_PRIORITY
        };
        //for(int i=1;i<=3;i++) {

        String selection = TodoContract.TodoEntry.COLUMN_NAME_PRIORITY + " = ?";
        //String[] selectionArgs = {String.valueOf(i)};

        String sortOrder = TodoContract.TodoEntry.COLUMN_NAME_DATA + " DESC ";
        try {
            cursor = db.query(
                    TodoContract.TodoEntry.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );
            while (cursor.moveToNext()){
                String content=cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_CONTENT));
                int data=cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_DATA));
                int intState=cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_STATE));
                int id=cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry._ID));

                String TAG="TEST QUERY: ";
                Log.d(TAG, "loadNotesFromDatabase: "+content);

                Note note=new Note(id);
                note.setContent(content);
                note.setDate(new Date(data));
                note.setState(State.from(intState));

                result.add(note);
            }
        }finally {
            if(cursor!=null){
                cursor.close();

            }
        }
        //}

        return result;

    }

    private void deleteNote(Note note) {
        // TODO 删除数据

        if (db == null) {
            return;
        }
        int rows = db.delete(TodoContract.TodoEntry.TABLE_NAME,
                TodoContract.TodoEntry._ID + "=?",
                new String[]{String.valueOf(note.id)});
        if (rows > 0) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private void updateNode(Note note) {
        // 更新数据
        TodoDbHelper mDbHelper=new TodoDbHelper(this);
        SQLiteDatabase db=mDbHelper.getWritableDatabase();
        db=mDbHelper.getWritableDatabase();

        String content=note.getContent();
        int date=(int)note.getDate().getTime();;
        //int state=(int)note.getState();
        long id=note.id;

        String selection= TodoContract.TodoEntry._ID ;
        String[] selectionArgs={String.valueOf(id)};

        ContentValues values=new ContentValues();
        values.put(TodoContract.TodoEntry.COLUMN_NAME_CONTENT, content);
        values.put(TodoContract.TodoEntry.COLUMN_NAME_DATA,date);
        values.put(TodoContract.TodoEntry.COLUMN_NAME_STATE, 0);

        int count=db.update(TodoContract.TodoEntry.TABLE_NAME,values,selection,selectionArgs);
    }

}
