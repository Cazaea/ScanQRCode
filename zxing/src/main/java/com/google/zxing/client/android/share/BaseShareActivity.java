package com.google.zxing.client.android.share;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.R;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.google.zxing.client.android.utils.PermissionUtil;

import java.util.regex.Pattern;

/**
 * @author Cazaea
 * @time 2017/12/14 10:22
 * @mail wistorm@sina.com
 */

public class BaseShareActivity extends Activity {

    private static final String TAG = BaseShareActivity.class.getSimpleName();

    private static final String SCHEME = "package";
    /**
     * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.1及之前版本)
     */
    private static final String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
    /**
     * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.2)
     */
    private static final String APP_PKG_NAME_22 = "pkg";
    /**
     * InstalledAppDetails所在包名
     */
    private static final String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
    /**
     * InstalledAppDetails类名
     */
    private static final String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";

    private static final int PICK_BOOKMARK = 0;
    private static final int PICK_CONTACT = 1;
    private static final int PICK_APP = 2;

    private QRCodeEncoder qrCodeEncoder;
    private static final int MAX_BARCODE_FILENAME_LENGTH = 24;
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");
    private static final String USE_VCARD_KEY = "USE_VCARD";

    public Bitmap appBitmap;
    public Bitmap bookMarkBitmap;
    public Bitmap contactsBitmap;

    public String content;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 判断联系人权限是否开启(Android6.0只收需要手动获取)
     */
    public boolean judgeContactAuthority() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 判断通讯录权限是否已经获取
            if (PermissionUtil.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                displayFrameworkBugMessageAndExit();
                return false;
            }
        }
        return true;
    }

    /**
     * 打开系统应用设置，开启权限
     */
    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("无法获取联系人信息，可能是相关权限未打开，请尝试打开再重试。");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showInstalledAppDetails(getApplicationContext(), getPackageName());
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * 调用系统InstalledAppDetails界面显示已安装应用程序的详细信息。
     * 对于Android 2.3（Api Level9）以上，使用SDK提供的接口； 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）。
     *
     * @param context
     * @param packageName 应用程序的包名
     */

    public static void showInstalledAppDetails(Context context, String packageName) {
        Intent intent = new Intent();
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= 9) { // 2.3（ApiLevel 9）以上，使用SDK提供的接口
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, packageName, null);
            intent.setData(uri);
        } else {
            // 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）
            // 2.2和2.1中，InstalledAppDetails使用的APP_PKG_NAME不同。
            final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22
                    : APP_PKG_NAME_21);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName(APP_DETAILS_PACKAGE_NAME,
                    APP_DETAILS_CLASS_NAME);
            intent.putExtra(appPkgName, packageName);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 分享应用
     */
    public void shareAPP() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setClassName(BaseShareActivity.this, AppPickerActivity.class.getName());
        startActivityForResult(intent, PICK_APP);
    }

    /**
     * 分享联系人
     */
    public void shareContact() {
        if (!judgeContactAuthority()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    /**
     * 分享书签
     */
    public void shareBookMark() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setClassName(BaseShareActivity.this, BookmarkPickerActivity.class.getName());
        startActivityForResult(intent, PICK_BOOKMARK);
    }

    /**
     * 分享剪切板
     */
    public Bitmap shareClipboard() {
        // Should always be true, because we grey out the clipboard button in onResume() if it's empty
        CharSequence text = ClipboardInterface.getText(BaseShareActivity.this);
        if (text == null) {
            return null;
        }
        return shareText(text.toString());
    }

    /**
     * 文字生成二维码
     * <p>
     * //     * @param text
     */
//    public Bitmap shareText(String text) {
//        if (text == null) {
//            return null; // Show error?
//        }
//        Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
//        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
//        intent.putExtra(Intents.Encode.DATA, text);
//        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
//        return createQRCode(intent);
//    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_BOOKMARK:
                    bookMarkBitmap = shareText(intent.getStringExtra("url"));
                    break;
                case PICK_APP:
                    // Browser.BookmarkColumns.URL
                    appBitmap = shareText(intent.getStringExtra("url"));
//                    createQRCodeCallBack.shareAPPOrBookMark();
                    break;
                case PICK_CONTACT:
                    // Data field is content://contacts/people/984
                    contactsBitmap = showContactAsBarcode(intent.getData());
//                    createQRCodeCallBack.shareContacts();
                    break;
            }
        }
    }

    /**
     * Bitmap对象置为空
     */
    private void resetInfo() {
        appBitmap = null;
        bookMarkBitmap = null;
        contactsBitmap = null;
        content = null;
    }

    public Bitmap shareText(String text) {
        Log.i(TAG, "Showing text as barcode: " + text);
        if (text == null) {
            return null; // Show error?
        }
        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, text);
        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
        return createQRCode(intent);
    }

    /**
     * Takes a contact Uri and does the necessary database lookups to retrieve that person's info,
     * then sends an Encode intent to render it as a QR Code.
     *
     * @param contactUri A Uri of the form content://contacts/people/17
     */
    private Bitmap showContactAsBarcode(Uri contactUri) {
        Log.i(TAG, "Showing contact URI as barcode: " + contactUri);
        if (contactUri == null) {
            return null; // Show error?
        }
        ContentResolver resolver = getContentResolver();

        String id;
        String name;
        boolean hasPhone;
        try (Cursor cursor = resolver.query(contactUri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;
        }

        // Don't require a name to be present, this contact might be just a phone number.
        Bundle bundle = new Bundle();
        if (name != null && !name.isEmpty()) {
            bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
        }

        if (hasPhone) {
            try (Cursor phonesCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + '=' + id,
                    null,
                    null)) {
                if (phonesCursor != null) {
                    int foundPhone = 0;
                    int phonesNumberColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int phoneTypeColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    while (phonesCursor.moveToNext() && foundPhone < Contents.PHONE_KEYS.length) {
                        String number = phonesCursor.getString(phonesNumberColumn);
                        if (number != null && !number.isEmpty()) {
                            bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
                        }
                        int type = phonesCursor.getInt(phoneTypeColumn);
                        bundle.putInt(Contents.PHONE_TYPE_KEYS[foundPhone], type);
                        foundPhone++;
                    }
                }
            }
        }

        try (Cursor methodsCursor = resolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + '=' + id,
                null,
                null)) {
            if (methodsCursor != null && methodsCursor.moveToNext()) {
                String data = methodsCursor.getString(
                        methodsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
                if (data != null && !data.isEmpty()) {
                    bundle.putString(ContactsContract.Intents.Insert.POSTAL, massageContactData(data));
                }
            }
        }

        try (Cursor emailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + '=' + id,
                null,
                null)) {
            if (emailCursor != null) {
                int foundEmail = 0;
                int emailColumn = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                while (emailCursor.moveToNext() && foundEmail < Contents.EMAIL_KEYS.length) {
                    String email = emailCursor.getString(emailColumn);
                    if (email != null && !email.isEmpty()) {
                        bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(email));
                    }
                    foundEmail++;
                }
            }
        }

        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.CONTACT);
        intent.putExtra(Intents.Encode.DATA, bundle);
        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());

        return createQRCode(intent);
    }

    private static String massageContactData(String data) {
        // For now -- make sure we don't put newlines in shared contact data. It messes up
        // any known encoding of contact data. Replace with space.
        if (data.indexOf('\n') >= 0) {
            data = data.replace("\n", " ");
        }
        if (data.indexOf('\r') >= 0) {
            data = data.replace("\r", " ");
        }
        return data;
    }

    /**
     * 生成二维码
     */
    private Bitmap createQRCode(Intent intent) {

        resetInfo();

        // This assumes the view is full screen, which is a good assumption
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int width = displaySize.x;
        int height = displaySize.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;

        Bitmap bitmap;

        try {
            boolean useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
            qrCodeEncoder = new QRCodeEncoder(this, intent, smallerDimension, useVCard);
            bitmap = qrCodeEncoder.encodeAsBitmap();
            if (bitmap == null) {
                Log.w(TAG, "Could not encode barcode");
                qrCodeEncoder = null;
                return null;
            }

            // 获取文本信息并显示
            if (intent.getBooleanExtra(Intents.Encode.SHOW_CONTENTS, true)) {
                // 文本内容
                content = qrCodeEncoder.getDisplayContents();
//                setTitle(qrCodeEncoder.getTitle());
            } else {
//                setTitle("");
            }
        } catch (WriterException e) {
            Log.w(TAG, "Could not encode barcode", e);
            qrCodeEncoder = null;
            bitmap = null;
        }

        return bitmap;
    }

}
