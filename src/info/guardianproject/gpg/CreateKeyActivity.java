package info.guardianproject.gpg;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CreateKeyActivity extends Activity {
	public static final String TAG = "CreateKeyActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.create_key);

		setNameAndEmail();

		// TODO figure out key/subkey logic, right now, just go with GPG default RSA+RSA
		//registerForContextMenu(findViewById(R.id.keyType));
		registerForContextMenu(findViewById(R.id.keySize));
		registerForContextMenu(findViewById(R.id.keyExpire));
		
		Button createKeyButton = (Button) findViewById(R.id.createKeyButton);
		createKeyButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String params = "<GnupgKeyParms format=\"internal\">\n";
				String keyName = ((EditText) findViewById(R.id.keyName)).getText().toString();
				params += "Name-Real: " + keyName + "\n";
				String keyEmail = ((EditText) findViewById(R.id.keyEmail)).getText().toString();
				params += "Name-Email: " + keyEmail + "\n";
				String keyComment = ((EditText) findViewById(R.id.keyComment)).getText().toString();
				params += "Name-Comment: " + keyComment + "\n";
				// TODO figure out key/subkey logic, right now, just go with GPG default RSA+RSA
				//String keyType = ((TextView) findViewById(R.id.keyType)).getText().toString();
				//params += "Key-Type: " + keyType + "\n";
				params += "Key-Type: RSA\n";
				String keySize = ((TextView) findViewById(R.id.keySize)).getText().toString();
				params += "Key-Length: " + keySize + "\n";
				// TODO the subkeys should be configurable
				params += "Subkey-Type: RSA\n";
				params += "Subkey-Length: " + keySize + "\n";
				String keyExpire = ((TextView) findViewById(R.id.keyExpire)).getText().toString();
				if (keyExpire.equals(getString(R.string.key_expire_one_month)))
					keyExpire = "1m";
				else if (keyExpire.equals(getString(R.string.key_expire_one_year)))
					keyExpire = "1y";
				else if (keyExpire.equals(getString(R.string.key_expire_two_years)))
					keyExpire = "2y";
				else if (keyExpire.equals(getString(R.string.key_expire_five_years)))
					keyExpire = "5y";
				else if (keyExpire.equals(getString(R.string.key_expire_ten_years)))
					keyExpire = "10y";
				else if (keyExpire.equals(getString(R.string.key_expire_never)))
					keyExpire = "0";
				params += "Expire-Date: " + keyExpire + "\n";
				params += "</GnupgKeyParms>\n";
				new CreateKeyTask(v.getContext()).execute(params);
			}
		});
	}

	private void setNameAndEmail() {
		String email = null;

		// get email address from first system account that looks like an email
		AccountManager manager = AccountManager.get(this);
		for (Account account : manager.getAccounts())
			if (account.name.contains("@") && account.name.contains(".")) {
				email = account.name;
				EditText keyEmail = (EditText) findViewById(R.id.keyEmail);
				keyEmail.setText(email);
				break;
			}
		if (email == null)
			return;

		// use that email to look up the name in Contacts
		final String[] projection = {
				Contacts.DISPLAY_NAME,
				CommonDataKinds.Email.DATA,
		};
		Cursor cursor = getContentResolver().query(
				CommonDataKinds.Email.CONTENT_URI,
				projection,
				CommonDataKinds.Email.DATA + " = ?",
				new String[]{email},
				null);
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
			String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
			EditText keyName = (EditText) findViewById(R.id.keyName);
			keyName.setText(name);
		}
		// TODO we might want to use Profile.DISPLAY_NAME_PRIMARY on API >= 11
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		switch (v.getId()) {
		case R.id.keyType:
			inflater.inflate(R.menu.key_type, menu);
			break;
		case R.id.keySize:
			inflater.inflate(R.menu.key_size, menu);
			break;
		case R.id.keyExpire:
			inflater.inflate(R.menu.key_expire, menu);
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.keyTypeRSA:
			return true;
		case R.id.keyTypeDSA:
			return true;
		case R.id.keyTypeElGamal:
			return true;
		case R.id.keySize1024:
			return true;
		case R.id.keySize2048:
			return true;
		case R.id.keySize4096:
			return true;
		case R.id.keySize8192:
			return true;
		case R.id.keyExpire1Month:
			return true;
		case R.id.keyExpire1Year:
			return true;
		case R.id.keyExpire2Years:
			return true;
		case R.id.keyExpire5Years:
			return true;
		case R.id.keyExpire10Years:
			return true;
		case R.id.keyExpireNever:
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public class CreateKeyTask extends AsyncTask<String, Void, Void> {
		private ProgressDialog dialog;

		public CreateKeyTask(Context c) {
			dialog = new ProgressDialog(c);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setTitle(R.string.dialog_generating_new_key_title);
			dialog.setMessage(getString(R.string.dialog_generating_new_key_msg));
		}

		@Override
		protected Void doInBackground(String... params) {
			Log.i(TAG, params[0]);
			GnuPG.context.genPgpKey(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void r) {
	        if (dialog.isShowing())
	            dialog.dismiss();
        }
	}
}