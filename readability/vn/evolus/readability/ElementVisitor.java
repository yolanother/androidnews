package vn.evolus.readability;

import org.jsoup.nodes.Element;

public interface ElementVisitor {
	void visit(Element element);
}
