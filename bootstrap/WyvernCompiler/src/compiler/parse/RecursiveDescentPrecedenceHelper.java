/**
 * 
 */
package compiler.parse;

import java.util.ArrayList;
import java.util.List;

import compiler.parse.RecursiveDescentAnalyzer.Rule;

/**
 * @author mikea_000
 *
 */
final class RecursiveDescentPrecedenceHelper {
	private final RecursiveDescentAnalyzer analyzer;
	private final List<RuleInfo> stack = new ArrayList<RuleInfo>();
	private int version, stackTop = -1;
	
	public RecursiveDescentPrecedenceHelper(RecursiveDescentAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	
	public void pushRuleScope(int tokenIndex, Production production, Rule rule) {
		++this.stackTop;
		
		RuleInfo info;
		if (this.stackTop == this.stack.size()) {
			this.stack.add(info = new RuleInfo());
		} else {
			// re-use the info object if we can
			info = this.stack.get(this.stackTop);
		}
		
		if (info.tokenIndex != tokenIndex
			|| info.rule != rule
			|| !production.equals(info.production)) {
			info.version = ++this.version;
		}
		info.tokenIndex = tokenIndex;
		info.rule = rule;
		info.production = production;
	}
	
	public void popRuleScope() {
		--this.stackTop;
	}
	
	public boolean allow(Production production, int tokenIndex) {
		// check down the stack from the top
		for (int i = this.stackTop; i >= 0; --i) {
			RuleInfo ruleInfo = this.stack.get(i);
			if (ruleInfo.tokenIndex != tokenIndex) {
				break;
			}
			
			if (!this.analyzer.allow(production, ruleInfo.rule, ruleInfo.production)) {
				return false;
			}
		}
		
		return true;
	}
	
	public int cacheVersion() {
		return this.stackTop >= 0 ? this.stack.get(this.stackTop).version : 0;
	}
	
	private static class RuleInfo {
		public int tokenIndex;
		public Production production;
		public Rule rule;
		public int version;
	}
}
