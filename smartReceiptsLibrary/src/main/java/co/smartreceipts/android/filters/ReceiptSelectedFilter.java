package co.smartreceipts.android.filters;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.model.Receipt;

public class ReceiptSelectedFilter implements Filter<Receipt> {

	public ReceiptSelectedFilter() {
		// empty
	}

	public ReceiptSelectedFilter(JSONObject json) throws JSONException {
		// empty
	}

	@Override
	public boolean accept(Receipt t) {
		return t.isSelected();
	}

	@Override
	public JSONObject getJsonRepresentation() throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(FilterFactory.CLASS_NAME, this.getClass().getName());
		return json;
	}

	@Override
	public List<Filter<Receipt>> getChildren() {
		return null;
	}

	@Override
	public int getNameResource() {
		return R.string.filter_name_receipt_selected;
	}

	@Override
	public FilterType getType() {
		return FilterType.Boolean;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getClass().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		// only compare by class since this filter has no parameters
		return (obj != null && getClass() == obj.getClass());
	}
}
