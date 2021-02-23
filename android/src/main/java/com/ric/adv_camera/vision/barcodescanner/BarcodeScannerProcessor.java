/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ric.adv_camera.vision.barcodescanner;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.ric.adv_camera.vision.VisionProcessorBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

/** Barcode Detector Demo. */
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>> {

  public interface BarcodeEventHandler {

    void onBarCodeRead(List<Barcode> barcodes,double avgFrameLatency);
  };

  private static final String TAG = "BarcodeProcessor";

  private final BarcodeScanner barcodeScanner;

  BarcodeEventHandler barcodeEventHandler;

  public void setBarcodeEventHandler(BarcodeEventHandler barcodeEventHandler) {
    this.barcodeEventHandler = barcodeEventHandler;
  }

  public void setEventSink(EventChannel.EventSink _eventSink) {
    this.eventSink = _eventSink;
  }

  private  EventChannel.EventSink eventSink = null;


  public BarcodeScannerProcessor(Context context,int barcodeFormats, boolean _debugMode) {
    super(context, _debugMode);
    // Note that if you know which format of barcode your app is dealing with, detection will be
    // faster to specify the supported barcode formats one by one, e.g.
    // new BarcodeScannerOptions.Builder()
    //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
    //     .build();
    barcodeScanner = BarcodeScanning.getClient(
             new BarcodeScannerOptions.Builder()
                 .setBarcodeFormats(barcodeFormats)
                 .build()
    );
  }

  @Override
  public void stop() {
    super.stop();
    barcodeScanner.close();
  }

  @Override
  protected Task<List<Barcode>> detectInImage(InputImage image) {
    return barcodeScanner.process(image);
  }



  @Override
  protected void onSuccess(
          @NonNull List<Barcode> barcodes, @Nullable double avgFrameLatency) {
    /*for (int i = 0; i < barcodes.size(); ++i) {
      Barcode barcode = barcodes.get(i);
      //graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
      logExtrasForTesting(barcode);
    }*/


    if(barcodeEventHandler != null) {
        barcodeEventHandler.onBarCodeRead(barcodes, avgFrameLatency);
    }

  }

  private static void logExtrasForTesting(Barcode barcode) {
    if (barcode != null) {
      Log.d(
          MANUAL_TESTING_LOG,
          String.format(
              "Detected barcode's bounding box: %s", barcode.getBoundingBox().flattenToString()));
      Log.d(
          MANUAL_TESTING_LOG,
          String.format(
              "Expected corner point size is 4, get %d", barcode.getCornerPoints().length));
      for (Point point : barcode.getCornerPoints()) {
        Log.d(
            MANUAL_TESTING_LOG,
            String.format("Corner point is located at: x = %d, y = %d", point.x, point.y));
      }
      Log.d(MANUAL_TESTING_LOG, "barcode display value: " + barcode.getDisplayValue());
      Log.d(MANUAL_TESTING_LOG, "barcode raw value: " + barcode.getRawValue());
      Barcode.DriverLicense dl = barcode.getDriverLicense();
      if (dl != null) {
        Log.d(MANUAL_TESTING_LOG, "driver license city: " + dl.getAddressCity());
        Log.d(MANUAL_TESTING_LOG, "driver license state: " + dl.getAddressState());
        Log.d(MANUAL_TESTING_LOG, "driver license street: " + dl.getAddressStreet());
        Log.d(MANUAL_TESTING_LOG, "driver license zip code: " + dl.getAddressZip());
        Log.d(MANUAL_TESTING_LOG, "driver license birthday: " + dl.getBirthDate());
        Log.d(MANUAL_TESTING_LOG, "driver license document type: " + dl.getDocumentType());
        Log.d(MANUAL_TESTING_LOG, "driver license expiry date: " + dl.getExpiryDate());
        Log.d(MANUAL_TESTING_LOG, "driver license first name: " + dl.getFirstName());
        Log.d(MANUAL_TESTING_LOG, "driver license middle name: " + dl.getMiddleName());
        Log.d(MANUAL_TESTING_LOG, "driver license last name: " + dl.getLastName());
        Log.d(MANUAL_TESTING_LOG, "driver license gender: " + dl.getGender());
        Log.d(MANUAL_TESTING_LOG, "driver license issue date: " + dl.getIssueDate());
        Log.d(MANUAL_TESTING_LOG, "driver license issue country: " + dl.getIssuingCountry());
        Log.d(MANUAL_TESTING_LOG, "driver license number: " + dl.getLicenseNumber());
      }
    }
  }

  public static  Map<String, Object> barcodeToMap(Barcode barcode) {
    Map<String, Object> barcodeMap = new HashMap<>();

    Rect bounds = barcode.getBoundingBox();
    if (bounds != null) {
      barcodeMap.put("left", (double) bounds.left);
      barcodeMap.put("top", (double) bounds.top);
      barcodeMap.put("width", (double) bounds.width());
      barcodeMap.put("height", (double) bounds.height());
    }

    List<double[]> points = new ArrayList<>();
    if (barcode.getCornerPoints() != null) {
      for (Point point : barcode.getCornerPoints()) {
        points.add(new double[]{(double) point.x, (double) point.y});
      }
    }
    barcodeMap.put("points", points);

    barcodeMap.put("rawValue", barcode.getRawValue());
    barcodeMap.put("displayValue", barcode.getDisplayValue());
    barcodeMap.put("format", barcode.getFormat());
    barcodeMap.put("valueType", barcode.getValueType());
    return barcodeMap;
  }


  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Barcode detection failed " + e);
    FirebaseCrashlytics.getInstance().recordException(e);

  }
}
