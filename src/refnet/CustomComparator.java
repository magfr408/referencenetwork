package refnet;

import java.math.BigDecimal;
import java.util.Comparator;

public class CustomComparator implements Comparator<Part> {

	@Override
	public int compare(Part o1, Part o2) {

			BigDecimal a = BigDecimal.valueOf(o1.getMeasureFrom());
			BigDecimal c = BigDecimal.valueOf(o2.getMeasureFrom());

			return a.compareTo(c);
	}
}