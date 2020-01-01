package kr.ac.ajou.esd.ocr;

import java.io.Serializable;
import java.util.List;

/**
 * OCR 이 처리된 후 모든 결과를 취합한 Data.
 */
public class ProcessedResult implements Serializable {
	private static final long serialVersionUID = 6899923902572946295L;

	// 인식한 차량 번호
	private String plateNumber;
	// 중간 중간 처리한 데이터
	private List<ProcessedData> processedDataList;
	// 전체 처리 시간 (ms)
	private long processingTime;

	public ProcessedResult(String plateNumber, List<ProcessedData> processedDataList, long processingTime) {
		this.plateNumber = plateNumber;
		this.processedDataList = processedDataList;
		this.processingTime = processingTime;
	}

	public String getPlateNumber() {
		return plateNumber;
	}

	public List<ProcessedData> getProcessedDataList() {
		return processedDataList;
	}

	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ProcessedResult{");
		sb.append("plateNumber='").append(plateNumber).append('\'');
		sb.append(", processedDataList=").append(processedDataList);
		sb.append(", processingTime=").append(processingTime);
		sb.append('}');
		return sb.toString();
	}
}