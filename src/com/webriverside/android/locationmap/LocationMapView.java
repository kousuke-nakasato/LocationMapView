package com.webriverside.android.locationmap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.webriverside.android.locationmap.util.AppConst;
import com.webriverside.android.locationmap.util.LocationHelper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LocationMapView extends MapActivity implements OnClickListener, LocationListener {

	private static final String LOGTAG_CLASS = LocationMapView.class.getSimpleName();

	// 画面コントロール
	private ProgressDialog    progressDialog;
	private TextView          lblCurrAddress;
	private EditText          edtSearchAddress;
	private Button            btnSearch;
	private Button            btnClose;
	// マップ関連コントロール
	private MapView           mapView;
	private LocationManager   locationManager;
	private LocationProvider  locationProvider;
	private MapController     mapController;
	private Geocoder          geocoder;
	// その他状態保持変数
	private String            currLocationProvider;
	private String            currentAddressName;
//	private LocationListener  locationListenerCurrentMap;
	private MyLocationOverlay overlay;

	// UI更新ハンドラ ※別スレッドから通知を受けてUI部品を更新する
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//プログレスダイアログを閉じる
			progressDialog.dismiss();
			
			switch(msg.what) {
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_END:
				//現在地取得: 正常終了ならマップを更新
				GeoPoint loadPoint = (GeoPoint)msg.obj;
				mapController.setCenter(loadPoint);
				mapController.animateTo(loadPoint);
				//現在地地図更新
				mapView.invalidate();
				//現在地ラベル更新
				lblCurrAddress.setText(currentAddressName == null ?
				      getString(R.string.warning_addressGetError) : currentAddressName);
				break;
			case AppConst.MESSAGE_WHAT_SEARCHADDRESS_END:
				//住所検索: 正常終了ならマップを更新
				Address address = (Address) msg.obj;
				GeoPoint searchPoint =
				   LocationHelper.getGeoPoint(address.getLatitude(), address.getLongitude());
				mapController.setCenter(searchPoint);
				mapController.animateTo(searchPoint);
				mapView.invalidate();
				break;
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_ERROR:
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_TOADDRESS_ERROR:
				//現在地取得: エラーならトースト表示 ※又はエラーダイアログ
				String locationError = (String) msg.obj;
				Toast.makeText(LocationMapView.this, locationError, Toast.LENGTH_SHORT).show();
				//現在地ラベル更新
				lblCurrAddress.setText(getString(R.string.warning_addressGetError));
				break;
			case AppConst.MESSAGE_WHAT_SEARCHADDRESS_ERROR:
	            //住所検索: エラーならアラートダイアログ表示
	            String searchError = (String) msg.obj;
	            final AlertDialog.Builder errorDialog =
	                          new AlertDialog.Builder(LocationMapView.this);
	            errorDialog.setTitle(R.string.errorDialog_searchResultTitle);
	            errorDialog.setMessage(searchError);
	            errorDialog.setPositiveButton(R.string.button_close,
	                                                   new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	                  //閉じるだけ
	               }
	            });
	            errorDialog.show();
	            break;
	         }
	      }
	   };

      public void onLocationChanged(Location location) {
         //移動された位置を取得しマップを更新する
         GeoPoint geoPoint = LocationHelper.getGeoPoint(location);
         mapController.setCenter(geoPoint);
         mapController.animateTo(geoPoint);
      }

      public void onProviderDisabled(String provider) {
      }
      public void onProviderEnabled(String provider) {
      }
      public void onStatusChanged(String provider, int status, Bundle extras) {
      }

	   //位置情報リスナー: 実機が指定範囲を超えて移動するとこのリスナーが呼び出される

      @Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.main);

	      //UI部品の取得とイベントリスナーの設定
	      lblCurrAddress   = (TextView)findViewById(R.id.lblCurrAddress);
	      edtSearchAddress = (EditText)findViewById(R.id.edtSearchAddress);
	      btnSearch        = (Button)findViewById(R.id.btnSearch);
	      btnClose         = (Button)findViewById(R.id.btnClose);
	      mapView          = (MapView)findViewById(R.id.map_view);
	      
	      // イベント登録
	      btnSearch.setOnClickListener(this);
	      btnClose.setOnClickListener(this);

	      // googleマップ設定
	      mapView.setBuiltInZoomControls(true);
	      
	      // overlay設定
//	      overlay = new MyLocationOverlay(this, mapView);
//	      overlay.onProviderEnabled(LocationManager.GPS_PROVIDER);
//		  overlay.enableMyLocation();
//		  mapView.getController().animateTo(overlay.getMyLocation()); // 現在位置を自動追尾する
//		  mapView.getOverlays().add(overlay);
//		  mapView.invalidate();
	        overlay = new MyLocationOverlay(this, mapView);
	        overlay.onProviderEnabled(LocationManager.GPS_PROVIDER);
	        overlay.enableMyLocation();
	        overlay.runOnFirstFix(new Runnable() {
	        	public void run() {
	        		mapView.getController().animateTo(overlay.getMyLocation()); // 現在位置を自動追尾する
	            }
	        });
	        mapView.getOverlays().add(overlay);
	        mapView.invalidate();

	        
	      // ジオコーダ生成
	      geocoder = new Geocoder(this, Locale.JAPAN);
	   }
	   
	   @Override
	   protected void onStart() {
	      super.onStart();

	      // 位置情報マネージャ取得
	      locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	      // 位置情報プロバイダ取得
	      locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
	      if (locationProvider == null) {
	         // ＧＰＳプロバイダが利用できなければ、ワイヤレスネットワークプロバイダを取得
	         locationProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
	         if (locationProvider == null) {
	            // ワイヤレスネットワークプロバイダが利用できなければ
	            //   エラーダイアログを表示してアプリを終了する
	            final AlertDialog.Builder fatalDialog =
	                                          new AlertDialog.Builder(LocationMapView.this);
	            fatalDialog.setTitle(R.string.fatalDialog_locationAvailableTitle);
	            fatalDialog.setMessage(R.string.fatalDialog_locationProviderNotAvailable);
	            fatalDialog.setPositiveButton(R.string.button_close,
	                                                 new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	                  //エラー終了
	                  finish();
	               }
	            });
	            fatalDialog.show();
	            return;
	         } else {
	            currLocationProvider = LocationManager.NETWORK_PROVIDER;
	         }
	      } else {
	         currLocationProvider = LocationManager.GPS_PROVIDER;
	      }
	      //更新間隔設定
	      locationManager.requestLocationUpdates(locationProvider.getName(),
		            LocationHelper.LOCATIONUPDATE_MINTIME,
		            LocationHelper.LOCATIONUPDATE_MINDISTANCE,
		            this);

	      //マップコントローラ取得
	      mapController = mapView.getController();
	      mapController.setZoom(LocationHelper.ZOOM_INIT);
	      //現在地取得
	      loadLastKnownPoint();
	   }

	   @Override
	   protected void onResume() {
	      super.onResume();

	      if (progressDialog != null && progressDialog.isShowing()) {
	         progressDialog.dismiss();
	      }
	   }

	   @Override
	   protected void onPause() {
	      super.onPause();

	      if (progressDialog != null && progressDialog.isShowing()) {
	         progressDialog.dismiss();
	      }
	   }

	   @Override
	   protected boolean isRouteDisplayed() {
	      return false;
	   }

	   @Override
	   public boolean onCreateOptionsMenu(Menu menu) {
	      super.onCreateOptionsMenu(menu);
	      getMenuInflater().inflate(R.menu.menu, menu);
	      return true;
	   }

	   @Override
	   public boolean onMenuItemSelected(int featureId, MenuItem item) {
		   return super.onMenuItemSelected(featureId, item);
	   }

	   @Override
	   public boolean onOptionsItemSelected(MenuItem item) {
		   Log.i("item id", String.valueOf(item.getItemId()));
		   Location lastKnownLocation = locationManager.getLastKnownLocation(currLocationProvider);
		   GeoPoint point             = LocationHelper.getGeoPoint(lastKnownLocation);
		   switch(item.getItemId()) {
		   case R.id.item1: // 現在地
			   mapController.setCenter(point);
			   mapController.animateTo(point);
			   mapView.invalidate();
			   break;
		   case R.id.item2:	// 拡大
			   mapController.zoomIn();
			   break;
		   case R.id.item3:	// 縮小
			   mapController.zoomOut();
			   break;
		   case R.id.item4:
			   break;
		   case R.id.item5: // 反転
			   mapController.setCenter(point);
			   mapController.animateTo(point);
			   mapView.invalidate();
			   break;
		   case R.id.item6: // 終了
			   finish();
			   break;
		   }
		   return super.onOptionsItemSelected(item);
	   }
	   
	   //現在地取得
	   private void loadLastKnownPoint() {
	      //現在地取得プログレスダイアログ表示
	      if (progressDialog == null || !progressDialog.isShowing()) {
	         progressDialog = ProgressDialog.show(this,
	               getString(R.string.progressDialog_getLocationTitle),
	               getString(R.string.progressDialog_getLocationMessage));
	      }

	      //別スレッドで実行
	      new Thread() {
	         @Override
	         public void run() {
	            Location lastKnownLocation =
	                locationManager.getLastKnownLocation(currLocationProvider);
	            Message msg = null;
	            if (lastKnownLocation != null) {
	               GeoPoint point = LocationHelper.getGeoPoint(lastKnownLocation);
	               //現在位置の住所取得
	               try {
	                  List<Address> list =
	                     geocoder.getFromLocation(
	                           LocationHelper.getGeocoderDouble(point.getLatitudeE6()),
	                           LocationHelper.getGeocoderDouble(point.getLongitudeE6()),
	                           1);
	                  Address address = list.get(0);
	                  //住所に変換
	                  currentAddressName = LocationHelper.convertAddressName(address);
	               } catch (IOException e) {
	                  //住所を取得できなかったら住所変換エラー
	                  Log.e(LOGTAG_CLASS, e.getMessage());
	                  msg = new Message();
	                  msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_TOADDRESS_ERROR;
	                  msg.obj = e.getMessage();
	                  handler.sendMessage(msg);
	                  //現在地をクリア
	                  currentAddressName = null;
	               }
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_END;
	               msg.obj = point;
	               handler.sendMessage(msg);
	            } else {
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_ERROR;
	               //エラーメッセージ
	               msg.obj = getString(R.string.warning_locationNotAvailable);
	               handler.sendMessage(msg);
	            }
	         }
	      }.start();
	   }

	   //--------------------------//
	   // クリックイベント                                       //
	   //--------------------------//
	   public void onClick(View v) {
		   // 押下ボタン判定
		   if(v == btnClose) {
			   // 閉じるボタン
			   finish();
		   } else if(v == btnSearch) {
			   // 検索ボタン
	            String searchAddress = edtSearchAddress.getText().toString();
	            if (searchAddress.length() > 0) {
	               //住所検索
	               searchAddressMap();
	            } else {
	               //未入力ならトースト表示
	               Toast.makeText(LocationMapView.this,
	                     R.string.warning_searchAddressRequired, Toast.LENGTH_SHORT).show();
	            }
		   }
	   }

	   // 住所検索
	   private void searchAddressMap() {
	      //住所検索プログレスダイアログ表示
	      if (progressDialog == null || !progressDialog.isShowing()) {
	         progressDialog = ProgressDialog.show(this,
	               getString(R.string.progressDialog_addressSearchTitle),
	               getString(R.string.progressDialog_addressSearchMessage));
	      }

	      //別スレッドで実行
	      new Thread() {
	         @Override
	         public void run() {
	            Message msg = null;
	            try {
	               List<Address> list =
	                  geocoder.getFromLocationName(edtSearchAddress.getText().toString(), 1);
	               Address address = list.get(0);
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_SEARCHADDRESS_END;
	               msg.obj = address;
	               handler.sendMessage(msg);
	            } catch (IOException e) {
	               Log.e(LOGTAG_CLASS, e.getMessage());
	               // エラーメッセージ
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_SEARCHADDRESS_ERROR;
	               msg.obj = e.getMessage();;
	               handler.sendMessage(msg);
	            }
	         }
	      }.start();
	   }
/*	   
	   private void setPosition() {
		   mapView.getController().animateTo(overlay.getMyLocation()); // 現在位置を自動追尾する
		   mapView.invalidate();
	   }
	   */
}
