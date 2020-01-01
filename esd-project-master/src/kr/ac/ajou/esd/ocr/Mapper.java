package kr.ac.ajou.esd.ocr;

import java.util.HashMap;
import java.util.Map;

/**
 * JNI를 통해 번화판을 인식하게 되는데 한글은 Java 로 전달하기 어려움이 있어서 알파벳과 한글을 Mapping 하기 위한 Util.
 * Mapper.transform();
 */
public class Mapper {
	private Map<Character, String> map = null;

	// Mapper initialize
	public Mapper() {
		make();
	}

	// 한글로 되어 있는 부분 변환하여 return
	public String transform(char[] chars) {
		StringBuilder builder = new StringBuilder();
		for (char c : chars) {
			if (map.containsKey(c))
				builder.append(map.get(c));
			else
				builder.append(c);
		}
		return builder.toString();
	}

	private void make() {
		map = new HashMap<Character, String>();
		map.put('a', "가");
		map.put('b', "나");
		map.put('c', "다");
		map.put('d', "라");
		map.put('e', "마");
		map.put('f', "거");
		map.put('g', "너");
		map.put('h', "더");
		map.put('i', "러");
		map.put('j', "머");
		map.put('k', "고");
		map.put('l', "노");
		map.put('m', "도");
		map.put('n', "로");
		map.put('o', "모");
		map.put('p', "구");
		map.put('q', "누");
		map.put('r', "두");
		map.put('s', "루");
		map.put('t', "무");
		map.put('u', "버");
		map.put('v', "서");
		map.put('w', "어");
		map.put('x', "저");
		map.put('y', "보");
		map.put('z', "소");
		map.put('A', "오");
		map.put('B', "조");
		map.put('C', "부");
		map.put('D', "수");
		map.put('E', "우");
		map.put('F', "주");
		map.put('G', "허");
		map.put('H', "하");
		map.put('I', "호");
		map.put('J', "바");
		map.put('K', "사");
		map.put('L', "아");
		map.put('M', "자");
		map.put('N', "배");
	}
}
