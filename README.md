# ImagePicker
一个图片选择器

### 效果图
![](demo.gif)

### Gradle引入
<pre>compile 'me.ywx.ImagePick:imagepick:1.0.0'</pre>

## 使用方法
#### 1.在需要调用选择图片的地方
```java
        Intent intent=new Intent(this,ImagePickActivity.class);
        intent.putExtra(PickFlag.PICK_COUNT,5);//传入最大能选择的图片数，不传此参数默认为1
        startActivityForResult(intent, PickFlag.PICK_PICTURE);
```
#### 2.接收返回的数据，在调用的地方
```java
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case PickFlag.PICK_PICTURE:
                if(resultCode==RESULT_OK){
                    List<String>list=data.getStringArrayListExtra(PickFlag.PICK_LIST);//返回一个图片路径的列表，存储着这次选择的图片的文件路径
                    Toast.makeText(this,list.size()+"",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
```
