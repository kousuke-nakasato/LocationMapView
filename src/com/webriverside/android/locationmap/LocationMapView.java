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

	// ��ʃR���g���[��
	private ProgressDialog    progressDialog;
	private TextView          lblCurrAddress;
	private EditText          edtSearchAddress;
	private Button            btnSearch;
	private Button            btnClose;
	// �}�b�v�֘A�R���g���[��
	private MapView           mapView;
	private LocationManager   locationManager;
	private LocationProvider  locationProvider;
	private MapController     mapController;
	private Geocoder          geocoder;
	// ���̑���ԕێ��ϐ�
	private String            currLocationProvider;
	private String            currentAddressName;
//	private LocationListener  locationListenerCurrentMap;
	private MyLocationOverlay overlay;

	// UI�X�V�n���h�� ���ʃX���b�h����ʒm���󂯂�UI���i���X�V����
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//�v���O���X�_�C�A���O�����
			progressDialog.dismiss();
			
			switch(msg.what) {
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_END:
				//���ݒn�擾: ����I���Ȃ�}�b�v���X�V
				GeoPoint loadPoint = (GeoPoint)msg.obj;
				mapController.setCenter(loadPoint);
				mapController.animateTo(loadPoint);
				//���ݒn�n�}�X�V
				mapView.invalidate();
				//���ݒn���x���X�V
				lblCurrAddress.setText(currentAddressName == null ?
				      getString(R.string.warning_addressGetError) : currentAddressName);
				break;
			case AppConst.MESSAGE_WHAT_SEARCHADDRESS_END:
				//�Z������: ����I���Ȃ�}�b�v���X�V
				Address address = (Address) msg.obj;
				GeoPoint searchPoint =
				   LocationHelper.getGeoPoint(address.getLatitude(), address.getLongitude());
				mapController.setCenter(searchPoint);
				mapController.animateTo(searchPoint);
				mapView.invalidate();
				break;
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_ERROR:
			case AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_TOADDRESS_ERROR:
				//���ݒn�擾: �G���[�Ȃ�g�[�X�g�\�� �����̓G���[�_�C�A���O
				String locationError = (String) msg.obj;
				Toast.makeText(LocationMapView.this, locationError, Toast.LENGTH_SHORT).show();
				//���ݒn���x���X�V
				lblCurrAddress.setText(getString(R.string.warning_addressGetError));
				break;
			case AppConst.MESSAGE_WHAT_SEARCHADDRESS_ERROR:
	            //�Z������: �G���[�Ȃ�A���[�g�_�C�A���O�\��
	            String searchError = (String) msg.obj;
	            final AlertDialog.Builder errorDialog =
	                          new AlertDialog.Builder(LocationMapView.this);
	            errorDialog.setTitle(R.string.errorDialog_searchResultTitle);
	            errorDialog.setMessage(searchError);
	            errorDialog.setPositiveButton(R.string.button_close,
	                                                   new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	                  //���邾��
	               }
	            });
	            errorDialog.show();
	            break;
	         }
	      }
	   };

      public void onLocationChanged(Location location) {
         //�ړ����ꂽ�ʒu���擾���}�b�v���X�V����
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

	   //�ʒu��񃊃X�i�[: ���@���w��͈͂𒴂��Ĉړ�����Ƃ��̃��X�i�[���Ăяo�����

      @Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.main);

	      //UI���i�̎擾�ƃC�x���g���X�i�[�̐ݒ�
	      lblCurrAddress   = (TextView)findViewById(R.id.lblCurrAddress);
	      edtSearchAddress = (EditText)findViewById(R.id.edtSearchAddress);
	      btnSearch        = (Button)findViewById(R.id.btnSearch);
	      btnClose         = (Button)findViewById(R.id.btnClose);
	      mapView          = (MapView)findViewById(R.id.map_view);
	      
	      // �C�x���g�o�^
	      btnSearch.setOnClickListener(this);
	      btnClose.setOnClickListener(this);

	      // google�}�b�v�ݒ�
	      mapView.setBuiltInZoomControls(true);
	      
	      // overlay�ݒ�
//	      overlay = new MyLocationOverlay(this, mapView);
//	      overlay.onProviderEnabled(LocationManager.GPS_PROVIDER);
//		  overlay.enableMyLocation();
//		  mapView.getController().animateTo(overlay.getMyLocation()); // ���݈ʒu�������ǔ�����
//		  mapView.getOverlays().add(overlay);
//		  mapView.invalidate();
	        overlay = new MyLocationOverlay(this, mapView);
	        overlay.onProviderEnabled(LocationManager.GPS_PROVIDER);
	        overlay.enableMyLocation();
	        overlay.runOnFirstFix(new Runnable() {
	        	public void run() {
	        		mapView.getController().animateTo(overlay.getMyLocation()); // ���݈ʒu�������ǔ�����
	            }
	        });
	        mapView.getOverlays().add(overlay);
	        mapView.invalidate();

	        
	      // �W�I�R�[�_����
	      geocoder = new Geocoder(this, Locale.JAPAN);
	   }
	   
	   @Override
	   protected void onStart() {
	      super.onStart();

	      // �ʒu���}�l�[�W���擾
	      locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	      // �ʒu���v���o�C�_�擾
	      locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
	      if (locationProvider == null) {
	         // �f�o�r�v���o�C�_�����p�ł��Ȃ���΁A���C�����X�l�b�g���[�N�v���o�C�_���擾
	         locationProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
	         if (locationProvider == null) {
	            // ���C�����X�l�b�g���[�N�v���o�C�_�����p�ł��Ȃ����
	            //   �G���[�_�C�A���O��\�����ăA�v�����I������
	            final AlertDialog.Builder fatalDialog =
	                                          new AlertDialog.Builder(LocationMapView.this);
	            fatalDialog.setTitle(R.string.fatalDialog_locationAvailableTitle);
	            fatalDialog.setMessage(R.string.fatalDialog_locationProviderNotAvailable);
	            fatalDialog.setPositiveButton(R.string.button_close,
	                                                 new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	                  //�G���[�I��
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
	      //�X�V�Ԋu�ݒ�
	      locationManager.requestLocationUpdates(locationProvider.getName(),
		            LocationHelper.LOCATIONUPDATE_MINTIME,
		            LocationHelper.LOCATIONUPDATE_MINDISTANCE,
		            this);

	      //�}�b�v�R���g���[���擾
	      mapController = mapView.getController();
	      mapController.setZoom(LocationHelper.ZOOM_INIT);
	      //���ݒn�擾
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
		   case R.id.item1: // ���ݒn
			   mapController.setCenter(point);
			   mapController.animateTo(point);
			   mapView.invalidate();
			   break;
		   case R.id.item2:	// �g��
			   mapController.zoomIn();
			   break;
		   case R.id.item3:	// �k��
			   mapController.zoomOut();
			   break;
		   case R.id.item4:
			   break;
		   case R.id.item5: // ���]
			   mapController.setCenter(point);
			   mapController.animateTo(point);
			   mapView.invalidate();
			   break;
		   case R.id.item6: // �I��
			   finish();
			   break;
		   }
		   return super.onOptionsItemSelected(item);
	   }
	   
	   //���ݒn�擾
	   private void loadLastKnownPoint() {
	      //���ݒn�擾�v���O���X�_�C�A���O�\��
	      if (progressDialog == null || !progressDialog.isShowing()) {
	         progressDialog = ProgressDialog.show(this,
	               getString(R.string.progressDialog_getLocationTitle),
	               getString(R.string.progressDialog_getLocationMessage));
	      }

	      //�ʃX���b�h�Ŏ��s
	      new Thread() {
	         @Override
	         public void run() {
	            Location lastKnownLocation =
	                locationManager.getLastKnownLocation(currLocationProvider);
	            Message msg = null;
	            if (lastKnownLocation != null) {
	               GeoPoint point = LocationHelper.getGeoPoint(lastKnownLocation);
	               //���݈ʒu�̏Z���擾
	               try {
	                  List<Address> list =
	                     geocoder.getFromLocation(
	                           LocationHelper.getGeocoderDouble(point.getLatitudeE6()),
	                           LocationHelper.getGeocoderDouble(point.getLongitudeE6()),
	                           1);
	                  Address address = list.get(0);
	                  //�Z���ɕϊ�
	                  currentAddressName = LocationHelper.convertAddressName(address);
	               } catch (IOException e) {
	                  //�Z�����擾�ł��Ȃ�������Z���ϊ��G���[
	                  Log.e(LOGTAG_CLASS, e.getMessage());
	                  msg = new Message();
	                  msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_TOADDRESS_ERROR;
	                  msg.obj = e.getMessage();
	                  handler.sendMessage(msg);
	                  //���ݒn���N���A
	                  currentAddressName = null;
	               }
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_END;
	               msg.obj = point;
	               handler.sendMessage(msg);
	            } else {
	               msg = new Message();
	               msg.what = AppConst.MESSAGE_WHAT_LASTKNOWNPOINT_ERROR;
	               //�G���[���b�Z�[�W
	               msg.obj = getString(R.string.warning_locationNotAvailable);
	               handler.sendMessage(msg);
	            }
	         }
	      }.start();
	   }

	   //--------------------------//
	   // �N���b�N�C�x���g                                       //
	   //--------------------------//
	   public void onClick(View v) {
		   // �����{�^������
		   if(v == btnClose) {
			   // ����{�^��
			   finish();
		   } else if(v == btnSearch) {
			   // �����{�^��
	            String searchAddress = edtSearchAddress.getText().toString();
	            if (searchAddress.length() > 0) {
	               //�Z������
	               searchAddressMap();
	            } else {
	               //�����͂Ȃ�g�[�X�g�\��
	               Toast.makeText(LocationMapView.this,
	                     R.string.warning_searchAddressRequired, Toast.LENGTH_SHORT).show();
	            }
		   }
	   }

	   // �Z������
	   private void searchAddressMap() {
	      //�Z�������v���O���X�_�C�A���O�\��
	      if (progressDialog == null || !progressDialog.isShowing()) {
	         progressDialog = ProgressDialog.show(this,
	               getString(R.string.progressDialog_addressSearchTitle),
	               getString(R.string.progressDialog_addressSearchMessage));
	      }

	      //�ʃX���b�h�Ŏ��s
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
	               // �G���[���b�Z�[�W
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
		   mapView.getController().animateTo(overlay.getMyLocation()); // ���݈ʒu�������ǔ�����
		   mapView.invalidate();
	   }
	   */
}
