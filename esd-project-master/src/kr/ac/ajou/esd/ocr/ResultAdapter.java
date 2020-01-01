package kr.ac.ajou.esd.ocr;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Result Activity 에서 상세 결과를 List 로 보여주기 위한 Adapter. View Holder 패턴을 이용.
 */
public class ResultAdapter extends ArrayAdapter<ProcessedData> {
	private static final String TAG = "ResultAdapter";

	// OCR 중간중간 처리된 List
	private List<ProcessedData> items;
	private Context context;
	// List 의 Row layout id
	private int layoutId;

	public ResultAdapter(Context context, int layoutId, List<ProcessedData> items) {
		super(context, layoutId, items);
		this.context = context;
		this.items = items;
		this.layoutId = layoutId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// ViewHodler Pattern 으로 구현하여 성능 이슈 제거
		View v = convertView;
		ViewHolder holder;

		// View Initialize. 초기 생성시에만 초기화를 해준다.
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(layoutId, null);
			holder = new ViewHolder();
			holder.tvTitle = (TextView) v.findViewById(R.id.tvTitle);
			holder.ivImage = (ImageView) v.findViewById(R.id.ivImage);

			v.setTag(holder);
		} else {
			holder = (ViewHolder) v.getTag();
		}

		// set data
		final ProcessedData vo = items.get(position);
		if (vo != null) {
			StringBuilder title = new StringBuilder(vo.getTitle());
			title.append(", processing time: ").append(vo.getElapsed()).append("ms");
			holder.tvTitle.setText(title.toString());
			try {
				holder.ivImage.setImageBitmap(BitmapUtil.uriToBitmap(context, vo.getImageUri()));
			} catch (IOException e) {
				Log.e(TAG, "Load image error!", e);
			}
		}
		return v;
	}

	class ViewHolder {
		TextView tvTitle;
		ImageView ivImage;
	}
}