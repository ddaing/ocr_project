package kr.ac.ajou.esd.ocr;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * 상세보기 시 화면. 처리된 과정을 ListView 로 확인 할 수 있다.
 */
public class ResultActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);

		// Intent 에 Result 가 없으면 화면 닫음.
		if (!getIntent().hasExtra("RESULT"))
			finish();
		ProcessedResult result = (ProcessedResult) getIntent().getSerializableExtra("RESULT");

		// Activity Title 에 인식한 차량번호 Set
		setTitle(result.getPlateNumber());
		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(new ResultAdapter(this, R.layout.item_result, result.getProcessedDataList()));
	}
}