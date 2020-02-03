package org.openrefine.browsing.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.util.StringUtils;

/**
 * Gathers statistics about distinct string representations
 * of values generated by an evaluable.
 * 
 * @todo this is not optimized so far (Map<String, Long> could
 * probably be made more memory efficient, the serialization of
 * the object could be controlled).
 * 
 * @author Antonin Delpeuch
 *
 */
public class StringValuesFacetState implements FacetState {
	
	private static final long serialVersionUID = 1L;
	final protected Facet _facet;
	final protected Map<String, Long> _counts;
	final protected long _errors;
	final protected long _blanks;
	final protected Evaluable   _evaluable;
    final protected int         _cellIndex;
    final protected ColumnModel _columnModel;
	
	public StringValuesFacetState(Facet facet, ColumnModel columnModel, Evaluable evaluable, int cellIndex) {
		this(facet, columnModel, evaluable, cellIndex, Collections.emptyMap(), 0L, 0L);
	}
	
	public StringValuesFacetState(
			Facet facet,
			ColumnModel model,
			Evaluable evaluable,
			int cellIndex,
			Map<String,Long> counts,
			long errors,
			long blanks) {
		_facet = facet;
		_columnModel = model;
		_counts = counts;
		_evaluable = evaluable;
		_cellIndex = cellIndex;
		_errors = errors;
		_blanks = blanks;
	}

	@Override
	public Facet getFacet() {
		return _facet;
	}
	
	public Map<String, Long> getCounts() {
		return _counts;
	}
	
	public long getErrorCount() {
		return _errors;
	}
	
	public long getBlankCount() {
		return _blanks;
	}

	@Override
	public StringValuesFacetState sum(FacetState other) {
		if (!(other instanceof StringValuesFacetState)) {
			throw new IllegalArgumentException("Summing two incompatible facet states");
		}
		StringValuesFacetState otherState = (StringValuesFacetState)other;
		Map<String, Long> newCounts = new HashMap<>(_counts);
		otherState.getCounts().entrySet().forEach(e -> {
			if (newCounts.containsKey(e.getKey())) {
				newCounts.put(e.getKey(), e.getValue() + newCounts.get(e.getKey()));
			} else {
				newCounts.put(e.getKey(), e.getValue());
			}
		});
		return new StringValuesFacetState(
				_facet, _columnModel, _evaluable, _cellIndex, newCounts,
				_errors + otherState.getErrorCount(),
				_blanks + otherState.getBlankCount());
	}

	@Override
	public StringValuesFacetState withRow(long rowId, Row row) {
		// Evaluate the expression on that row
		Properties bindings = ExpressionUtils.createBindings();
		ExpressionUtils.bind(bindings, _columnModel, row, rowId, null, row.getCell(_cellIndex));
		Object value = _evaluable.evaluate(bindings);
		if (ExpressionUtils.isError(value)) {
            return new StringValuesFacetState(
            		_facet, _columnModel, _evaluable, _cellIndex, _counts,
            		_errors + 1, _blanks);
        } else if (ExpressionUtils.isNonBlankData(value)) {
        	String valueStr = StringUtils.toString(value);
        	Map<String, Long> newCounts = new HashMap<>(_counts);
        	if (_counts.containsKey(valueStr)) {
        		newCounts.put(valueStr, _counts.get(valueStr) + 1);
        	} else {
        		newCounts.put(valueStr, 1L);
        	}
        	return new StringValuesFacetState(
        			_facet, _columnModel, _evaluable, _cellIndex, newCounts,
        			_errors, _blanks);
        } else {
        	return new StringValuesFacetState(
            		_facet, _columnModel, _evaluable, _cellIndex, _counts,
            		_errors, _blanks + 1);
        }
	}

	@Override
	public FacetState withRecord(Record record, List<Row> rows) {
		throw new IllegalStateException("records mode not implemented");
	}

}