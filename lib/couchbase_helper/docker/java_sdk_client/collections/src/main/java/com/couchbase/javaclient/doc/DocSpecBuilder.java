package com.couchbase.javaclient.doc;

public class DocSpecBuilder {
	int _num_ops;
	int _percent_create;
	int _percent_update;
	int _percent_delete;
	String _load_pattern;
	int _startSeqNum;
	String _prefix;
	String _suffix;
	String _template;

	public DocSpecBuilder() {
	}

	public DocSpec buildDocSpec() {
		return new DocSpec(_num_ops, _percent_create, _percent_update, _percent_delete, _load_pattern, _startSeqNum,
				_prefix, _suffix, _template);
	}

	public DocSpecBuilder loadPattern(String _load_pattern) {
		this._load_pattern = _load_pattern;
		return this;
	}

	public DocSpecBuilder numOps(int _num_ops) {
		this._num_ops = _num_ops;
		return this;
	}

	public DocSpecBuilder percentCreate(int _percent_create) {
		this._percent_create = _percent_create;
		return this;
	}

	public DocSpecBuilder percentDelete(int _percent_delete) {
		this._percent_delete = _percent_delete;
		return this;
	}

	public DocSpecBuilder percentUpdate(int _percent_update) {
		this._percent_update = _percent_update;
		return this;
	}

	public DocSpecBuilder prefix(String _prefix) {
		this._prefix = _prefix;
		return this;
	}

	public DocSpecBuilder startSeqNum(int _startSeqNum) {
		this._startSeqNum = _startSeqNum;
		return this;
	}

	public DocSpecBuilder suffix(String _suffix) {
		this._suffix = _suffix;
		return this;
	}

	public DocSpecBuilder template(String _template) {
		this._template = _template;
		return this;
	}

}
