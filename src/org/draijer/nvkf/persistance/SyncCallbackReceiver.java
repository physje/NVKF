package org.draijer.nvkf.persistance;

import android.os.Bundle;

public interface SyncCallbackReceiver {

	public void onSyncCallback(int resultCode, Bundle resultData);

}
