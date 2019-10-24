package com.sicnu.cs.zzy.classificationtimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.orhanobut.logger.Logger;
import com.sicnu.cs.zzy.classificationtimer.javaBean.ProcessInfo;
import com.sicnu.cs.zzy.classificationtimer.utils.AppManager;
import com.sicnu.cs.zzy.classificationtimer.utils.MyDBOpenHelper;
import com.sicnu.cs.zzy.classificationtimer.utils.UsageStatsUtils;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity {
    private SQLiteDatabase db;
    private Cursor watchlist_cursor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private PopupWindow popupWindow;
    private PackageManager packageManager;
    private AppListAdapter appListAdapter;
    private MyAdapter adapter;
    private ProcessInfo current_appinfo;
    private ArrayList<ProcessInfo> appInfoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        AppManager.getInstance().addActivity(this);
        initData();
        initView();
    }


    @Override
    protected void onDestroy() {
        AppManager.getInstance().finishActivity(this);
        super.onDestroy();
    }


    /**
     * 右上角
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_menu,menu);
        return true;
    }


    /**
     * 弹出菜单响应
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.it_add:
                initPopWindow(getWindow().getDecorView());
                break;
            case R.id.it_reset:
                editor.putBoolean("isChildren",false);
                db.execSQL("delete from killlist");
                Toast.makeText(SettingActivity.this,"成功清除死亡列表",Toast.LENGTH_SHORT).show();
                break;
            case R.id.it_chageAge:
                final EditText inputServer = new EditText(SettingActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setHint("目前的年龄阈值为 ："+MyService.LIMIT_AGE+"岁");
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setTitle("请输入更改后的年龄")
                        .setCancelable(false)
                        .setView(inputServer)
                        .setNegativeButton("取消",null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int age = Integer.valueOf(inputServer.getText().toString());
                                if(age>0 && age<50){
                                    MyService.LIMIT_AGE = age;
                                    Toast.makeText(SettingActivity.this,"修改成功！ 年龄阈值为 "+MyService.LIMIT_AGE+" 岁",Toast.LENGTH_LONG).show();
                                }else{
                                    Toast.makeText(SettingActivity.this,"请输入合理的数值",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                builder.show();
                break;
            case R.id.it_chageTime:
                final EditText input = new EditText(SettingActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setHint("目前的时间阈值为 ："+MyService.LIMIT_TIME/12+"分");
                AlertDialog.Builder builder2 = new AlertDialog.Builder(SettingActivity.this);
                builder2.setTitle("请输入更改后的时间")
                        .setCancelable(false)
                        .setView(input)
                        .setNegativeButton("取消",null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int time = Integer.valueOf(input.getText().toString());
                                MyService.LIMIT_TIME = time*12;
                                Toast.makeText(SettingActivity.this,"修改成功！ 时间阈值为 "+MyService.LIMIT_TIME/12+" 分",Toast.LENGTH_LONG).show();
                            }
                        });
                builder2.show();
                break;
        }
        return true;
    }


    /**
     * 初始化界面
     */
    private void initView(){
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_add));
        StatusBarUtil.setColor(this,R.color.colorBlack,50);
    }


    /**
     * 初始化弹出界面
     */
    private void initPopWindow(View v){
        View view = LayoutInflater.from(this).inflate(R.layout.popwindow_applist,null,false);
        final EditText et_pop_time = view.findViewById(R.id.et_pop_time);
        Button btn_pop_cancle = view.findViewById(R.id.btn_pop_cancle);
        Button btn_pop_confirm = view.findViewById(R.id.btn_pop_confirm);
        RecyclerView pop_recyclerView = view.findViewById(R.id.pop_recyclerView);
        popupWindow = new PopupWindow(view,ViewGroup.LayoutParams.MATCH_PARENT,1250,true);
        popupWindow.setAnimationStyle(R.anim.anim_pop);
        popupWindow.setTouchable(true);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                darkenBackground(1f);
            }
        });
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区 域无法dismiss
            }
        });
        btn_pop_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_appinfo = null;
                popupWindow.dismiss();
            }
        });
        btn_pop_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(et_pop_time.getText().toString().equals("")
                || current_appinfo == null){
                    et_pop_time.setText("");
                    et_pop_time.setHint("请选择应用并输入限制时间");
                }else{
                    int use_time = Integer.valueOf(et_pop_time.getText().toString());
                    use_time = use_time * 60 * 1000;
                    Cursor cursor = db.rawQuery("select * from watchlist WHERE packgename= ?",
                            new String[]{current_appinfo.getPackageName()});
                    if(cursor != null && cursor.getCount() == 1){
                        et_pop_time.setText("");
                        et_pop_time.setHint("监控列表中已经存在该应用");
                    }else{
                        db.execSQL("insert into watchlist(packgename,usetime) values (?,?)",
                                new String[]{current_appinfo.getPackageName(),use_time+""});
                        adapter.swapCursor(db.rawQuery("select * from watchlist",null));
                        current_appinfo = null;
                        popupWindow.dismiss();
                    }
                }
            }
        });


        appListAdapter.setSelectedPosition(-5);
        //显示应用列表
        pop_recyclerView.setLayoutManager(new GridLayoutManager(this,3,RecyclerView.VERTICAL, false));
        pop_recyclerView.setAdapter(appListAdapter);

        //在底部显示
        darkenBackground(0.5f);
        popupWindow.showAtLocation(v, Gravity.BOTTOM,0,0);
    }


    /**
     * 初始化数据库以及得到表数据
     */
    private void initData(){
        packageManager = getPackageManager();
        MyDBOpenHelper DBhelper = new MyDBOpenHelper(this,MyDBOpenHelper.DB_NAME,null,1);
        db = DBhelper.getWritableDatabase();
        watchlist_cursor = db.rawQuery("select * from watchlist",null);
        sharedPreferences = getSharedPreferences("Monitor",MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //监控列表适配器
        adapter = new MyAdapter(watchlist_cursor);
        //应用列表适配器
        appInfoList = UsageStatsUtils.getAllApplications(this);
        appListAdapter = new AppListAdapter(appInfoList);
    }


    /**
     * 改变背景透明度
     */
    private void darkenBackground(Float bgcolor) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgcolor;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);
    }


    /**
     * 获取屏幕宽度
     */
    private int getScreenWidth(){
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        return width;
    }


    /**
     * 显示监控列表的适配器
     * adapter class
     */
    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{
        private Cursor cursor;


        public MyAdapter(Cursor cursor) {
            this.cursor = cursor;
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list,parent,false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }


        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            if (!cursor.moveToPosition(position)){
                return;
            }
            String packgeName = cursor.getString(cursor.getColumnIndex("packgename"));
            String useDuration = cursor.getString(cursor.getColumnIndex("usetime"));
            int use_Duration = Integer.valueOf(useDuration)/60000;
            ProcessInfo info = new ProcessInfo();
            info.setPackageName(packgeName);
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packgeName, 0);//根据包名获取相关应用的信息
                String name = applicationInfo.loadLabel(packageManager).toString();
                Drawable icon = applicationInfo.loadIcon(packageManager);
                info.setName(name);
                info.setIcon(icon);
                int flags = applicationInfo.flags;
                if((flags & ApplicationInfo.FLAG_SYSTEM)==ApplicationInfo.FLAG_SYSTEM){
                    //系统进程
                    info.setUser(false);
                }else{
                    //用户进程
                    info.setUser(true);
                }
            }catch (PackageManager.NameNotFoundException e) {
                //某些系统进程没有名称和图标,会走此异常
                info.setName(packgeName);
                info.setIcon(getDrawable(R.drawable.ic_default));
                info.setUser(false);
                //e.printStackTrace();
                Logger.t("getRunningProcesses").e("系统进程没有名称和图标","PackageManager.NameNotFoundException");
            }
            holder.appName.setText(info.getName());
            holder.useDuration.setText("限制时间（规定年龄以下）"+use_Duration+" 分钟");
            holder.imageView.setImageDrawable(info.getIcon());
            holder.itemView.setTag(info);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //弹出菜单
                    final ProcessInfo processInfo = (ProcessInfo)holder.itemView.getTag();
                    PopupMenu popupMenu = new PopupMenu(SettingActivity.this,v);
                    popupMenu.getMenuInflater().inflate(R.menu.pop_menu,popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case R.id.it_delete:
                                    AlertDialog.Builder builder_delete = new AlertDialog.Builder(SettingActivity.this);
                                    builder_delete.setTitle("删除")
                                            .setMessage("确定要删除？")
                                            .setCancelable(false)
                                            .setNegativeButton("取消",null)
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    db.execSQL("delete from watchlist where packgename=?",
                                                            new String[]{processInfo.getPackageName()});
                                                    swapCursor(db.rawQuery("select * from watchlist",null));
                                                }
                                            });
                                    builder_delete.show();
                                    break;
                                case R.id.it_update:
                                    final EditText inputServer = new EditText(SettingActivity.this);
                                    inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                                    builder.setTitle("请输入更改后的时间(min)--")
                                            .setCancelable(false)
                                            .setView(inputServer)
                                            .setNegativeButton("取消",null)
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    int update_time = Integer.valueOf(inputServer.getText().toString());
                                                    update_time = update_time * 60 * 1000;
                                                    db.execSQL("update watchlist set usetime=? where packgename=?",
                                                            new String[]{update_time+"",processInfo.getPackageName()});
                                                    swapCursor(db.rawQuery("select * from watchlist",null));
                                                }
                                            });
                                    builder.show();
                                    break;
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                    return true;
                }
            });
        }


        @Override
        public int getItemCount() {
            if(cursor == null){
                return 0;
            }
            int a = cursor.getCount();
            return cursor.getCount();
        }


        public class ViewHolder extends RecyclerView.ViewHolder{
            TextView appName, useDuration;
            ImageView imageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                appName = itemView.findViewById(R.id.appName);
                useDuration = itemView.findViewById(R.id.useDuration);
                imageView = itemView.findViewById(R.id.img);
            }
        }


        /**
         * 更新列表
         */
        public void swapCursor(Cursor newCursor){
            if(this.cursor!=null){
                this.cursor.close();
            }
            this.cursor = newCursor;
            if(newCursor != null){
                this.notifyDataSetChanged();
            }
        }
    }


    /**
     * 显示应用列表的适配器（增加监控列表时需用到）
     * adapter class
     */
    class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder>{
        private ArrayList<ProcessInfo> list = new ArrayList<>();
        private int leftRight = 10;  //左右间隔
        private int topBottom = 10;  //上下间隔
        private int spanCount = 4;   //间隔数（列数+1）
        private int selectedPosition = -5; //默认一个参数


        public AppListAdapter(ArrayList<ProcessInfo> list) {
            this.list = list;
        }


        public void setSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.applist,parent,false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }


        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if(list == null){
                return ;
            }
            ProcessInfo appInfo = list.get(position);
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(0xD8BFD8FF);
            }else{
                holder.itemView.setBackgroundColor(Color.WHITE);
            }

            //获取屏幕宽度
            int screenWidth = getScreenWidth();
            //获取单张图片宽度
            int itemImgWidth = (screenWidth - leftRight * spanCount) / (spanCount-1);
            //设置view宽高
            ViewGroup.LayoutParams params = holder.pop_layout.getLayoutParams();
            params.width = itemImgWidth;
            params.height = itemImgWidth;
            holder.pop_layout.setLayoutParams(params);

            holder.app_name.setText(appInfo.getName());
            holder.app_icon.setImageDrawable(appInfo.getIcon());
            holder.itemView.setTag(appInfo);
            final int fposition = position;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ProcessInfo info = (ProcessInfo) v.getTag();
                    current_appinfo = info;
                    selectedPosition = fposition;
                    notifyDataSetChanged();  //刷新当前点击item
                }
            });
        }


        @Override
        public int getItemCount() {
            if(list != null){
                return list.size();
            }
            return 0;
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView app_name;
            ImageView app_icon;
            LinearLayout pop_layout;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                pop_layout = itemView.findViewById(R.id.pop_layout);
                app_name = itemView.findViewById(R.id.app_name);
                app_icon = itemView.findViewById(R.id.app_icon);
            }
        }
    }
}
