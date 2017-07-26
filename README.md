## DragBadgeView
仿QQ可拖拽控件
## 样例演示
![](pic/sample.gif)
## 引入
### 添加依赖
```
dependencies {
	compile 'com.fendoudebb.view:dragbadgeview:1.0.1'
}
``` 
### xml配置
```
<com.fendoudebb.view.DragBadgeView
        android:id="@+id/drag_view"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:paddingBottom="2dp"
        android:paddingLeft="5dp"
        android:paddingRight="5dp"
        android:paddingTop="2dp"/>
```
## 回调
```java
mDragBadgeView.setOnDragBadgeViewListener(new DragBadgeView.OnDragBadgeViewListener() {
    @Override
    public void onDisappear(String text) {
        Toast.makeText(getApplicationContext(), text + "条信息隐藏!", Toast.LENGTH_SHORT).show();
    }
});
```
## 设置字体大小
### xml,添加namespace
```
xmlns:app="http://schemas.android.com/apk/res-auto"
```
```java
app:textSize="12sp"
```
### java代码设置
```java
mDragBadgeView.setBgColor(Color.BLUE);
```
