# ImagePicker
一个图片选择器

### 效果图
![](demo.gif)

### Gradle引入
<pre>compile 'me.ywx.ImagePick:imagepick:1.0.2'</pre>

## 使用方法
#### 1.在配置文件申明权限
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
#### 2.在需要调用选择图片的地方
```java
        Intent intent=new Intent(this,ImagePickActivity.class);
        intent.putExtra(PickFlag.PICK_COUNT,5);//传入最大能选择的图片数，不传此参数默认为1
        startActivityForResult(intent, PickFlag.PICK_PICTURE);
```
#### 3.接收返回的数据，在调用的地方
```java
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case PickFlag.PICK_PICTURE:
                if(resultCode==RESULT_OK){
                    //返回一个图片路径的列表，存储着这次选择的图片的文件路径
                    List<String>list=data.getStringArrayListExtra(PickFlag.PICK_LIST);
                    Toast.makeText(this,list.size()+"",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
```
<pre>ps.1.目前不支持查看原图</pre>
