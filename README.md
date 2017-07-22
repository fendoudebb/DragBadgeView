## DragBadgeView
仿QQ可拖拽控件
## 样例演示
![](pic/sample.gif)
## 引入
### 添加依赖
```
dependencies {
	compile 'com.fendoudebb.view:dragbadgeview:1.0.0'
}
``` 
### xml配置
```
<com.fendoudebb.view.DragBadgeView
    android:id="@+id/drag_view"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"/>
```
## 回调
```java
mDragBadgeView.setOnDragBadgeViewListener(new DragBadgeView.OnDragBadgeViewListener() {
    @Override
    public void onDisappear(String text) {
        Toast.makeText(parent.getContext().getApplicationContext(), text + "条信息隐藏!", Toast.LENGTH_SHORT).show();
    }
});
```
