package com.example.yue.imagepicker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.imagepicker.ImagePickActivity;
import com.example.imagepicker.PickFlag;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent=new Intent(this,ImagePickActivity.class);
        intent.putExtra(PickFlag.PICK_COUNT,5);
        startActivityForResult(intent, PickFlag.PICK_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case PickFlag.PICK_PICTURE:
                if(resultCode==RESULT_OK){
                    List<String>list=data.getStringArrayListExtra(PickFlag.PICK_LIST);
                    Toast.makeText(this,list.size()+"",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
