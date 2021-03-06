package edu.scse.tracehub.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.scse.tracehub.R;
import edu.scse.tracehub.util.PathSmoothTool;

public class HomeFragment extends Fragment implements LocationSource {
    private HomeViewModel homeViewModel;
    //地图控件
    private TextureMapView textureMapView;
    private AMap aMap;
    public static final LatLng TIANJIN = new LatLng(39.13,117.2);// 天津市经纬度
    protected static CameraPosition cameraPosition;
    //存储轨迹
    public List<LatLng> latLngs = new ArrayList<LatLng>();

    //声明AMapLocationClient类对象，定位发起端
    private AMapLocationClient mLocationClient = null;
    //声明mLocationOption对象，定位参数
    public AMapLocationClientOption mLocationOption = null;
    //声明mListener对象，定位监听器
    private OnLocationChangedListener mListener = null;
    //标识，用于判断是否只显示一次定位信息和用户重新定位
    private boolean isFirstLoc = true;
    private boolean isTrace = false;
    //列表
    private Button mBtnInputFragment2;
    private Button mphoto;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.navigation_home, container, false);
        //列表，上传切换
        final View view = inflater.inflate(R.layout.navigation_home, container, false);
        mBtnInputFragment2 = view.findViewById(R.id.btn_list);
        mphoto =  view.findViewById(R.id.btn_shangchuan);
        mBtnInputFragment2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Navigation.findNavController(getView()).navigate(R.id.action_navigation_home_to_list3);

            }
        });
        mphoto.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),photo.class);
                startActivity(intent);
            }
        });
        return view;
    }
    LatLng getTarget() {
        return TIANJIN;
    }
    CameraPosition getCameraPosition() {
        return cameraPosition;
    }
    void setCameraPosition(CameraPosition cameraPosition) {
        cameraPosition = cameraPosition;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //地图
        textureMapView = (TextureMapView) getView().findViewById(R.id.map);
        if (textureMapView != null) {
            textureMapView.onCreate(savedInstanceState);
            aMap = textureMapView.getMap();
            UiSettings settings = aMap.getUiSettings();
            settings.setMyLocationButtonEnabled(true);
            aMap.setMyLocationEnabled(true);
            aMap.setLocationSource(this);
            location();
            if (getCameraPosition() == null) {
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(getTarget(), 10, 0, 0)));
            }else {
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(getCameraPosition()));
            }
        }
        UiSettings settings = aMap.getUiSettings();
        //设置了定位的监听
        aMap.setLocationSource(this);
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);
        //显示定位层并且可以触发定位,默认是flase
        aMap.setMyLocationEnabled(true);
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色 
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        aMap.setMyLocationStyle(myLocationStyle);
        //开始定位
        location();

        Button button = (Button) getActivity().findViewById(R.id.togglebutton);
        //开始结束按钮监听器
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTrace=!isTrace;
                //点击stop进行截屏
                if(isTrace==false)
                {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("消息")
                            .setMessage("记录轨迹结束，已为您自动截屏")
                            .setPositiveButton("确定",null)
                            .show();
                    aMap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
                        @Override
                        public void onMapScreenShot(Bitmap bitmap) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                            try {
                                // 保存在SD卡根目录下，图片为png格式。
                                //目录为手机SD/Pictures/Screenshots/test_20210514xxxx.png
                                FileOutputStream fos = new FileOutputStream(
                                        Environment.getExternalStorageDirectory() + "/Pictures/Screenshots/test_"
                                                + sdf.format(new Date()) + ".png");
                                boolean ifSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                try {
                                    fos.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (ifSuccess)
                                    Toast.makeText(getContext(), "截屏成功", Toast.LENGTH_SHORT).show();
                                else {
                                    Toast.makeText(getContext(), "截屏失败", Toast.LENGTH_SHORT).show();
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onMapScreenShot(Bitmap bitmap, int i) {

                        }

                    });
                }else{
                    new AlertDialog.Builder(getActivity())
                            .setTitle("消息")
                            .setMessage("记录轨迹开始")
                            .setPositiveButton("确定",null)
                            .show();
                }

            }
        });
    }
    private void location() {
        //初始化定位
        mLocationClient = new AMapLocationClient(this.getContext());
        //设置定位回调监听
        AMapLocationListener aMapLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息
                        aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                        aMapLocation.getLatitude();//获取纬度
                        aMapLocation.getLongitude();//获取经度
                        aMapLocation.getAccuracy();//获取精度信息
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date(aMapLocation.getTime());
                        df.format(date);//定位时间
                        aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                        aMapLocation.getCountry();//国家信息
                        aMapLocation.getProvince();//省信息
                        aMapLocation.getCity();//城市信息
                        aMapLocation.getDistrict();//城区信息
                        aMapLocation.getStreet();//街道信息
                        aMapLocation.getStreetNum();//街道门牌号信息
                        aMapLocation.getCityCode();//城市编码
                        aMapLocation.getAdCode();//地区编码

                        // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                        if (isFirstLoc) {
                            //设置缩放级别
                            aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                            //将地图移动到定位点
                            aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));
                            //添加图钉
                            //  aMap.addMarker(getMarkerOptions(amapLocation));
                            //获取定位信息
                            StringBuffer buffer = new StringBuffer();
                            buffer.append(aMapLocation.getCountry() + ""
                                    + aMapLocation.getProvince() + ""
                                    + aMapLocation.getCity() + ""
                                    + aMapLocation.getProvince() + ""
                                    + aMapLocation.getDistrict() + ""
                                    + aMapLocation.getStreet() + ""
                                    + aMapLocation.getStreetNum());
                            Toast.makeText(getContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                            isFirstLoc = false;
                        }
                        //点击定位按钮 能够将地图的中心移动到定位点
                        mListener.onLocationChanged(aMapLocation);
                        //画轨迹
                        if (isTrace) {
                            PolylineOptions options=new PolylineOptions();
                            latLngs.add(new LatLng(aMapLocation.getLatitude(),aMapLocation.getLongitude()));
                            PathSmoothTool mpathSmoothTool = new PathSmoothTool();
                            mpathSmoothTool.setIntensity(4);
                            List<LatLng> pathoptimizeList = mpathSmoothTool.pathOptimize(latLngs);
                            Polyline line = aMap.addPolyline(options.
                                    addAll(pathoptimizeList).width(10).color(Color.argb(255, 1, 1, 1)));
                            line.setColor(0xFFe495c7);
                            line.setVisible(true);
                        }
                    } else {
                        //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "location Error, ErrCode:"
                                + aMapLocation.getErrorCode() + ", errInfo:"
                                + aMapLocation.getErrorInfo());
                    }
                }
            }
        };
        mLocationClient.setLocationListener(aMapLocationListener);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        //mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }
    @Override
    public void onResume() {
        super.onResume();
        textureMapView.onResume();
    }

    /**
     * CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
     *         @Override
     *         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
     *
     *
     *         }
     *     };
     *     toggleButton.setOnCheckedChangeListener(checkedChangeListener);
     *    此处为togglebutton按钮切换使用方法
     */

    /**
     * 方法必须重写
     */
    @Override
    public void onPause() {
        super.onPause();
        textureMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        textureMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setCameraPosition(aMap.getCameraPosition());
        super.onDestroy();
        textureMapView.onDestroy();
        mLocationClient.stopLocation();//停止定位
        mLocationClient.onDestroy();//销毁定位客户端。
    }
}