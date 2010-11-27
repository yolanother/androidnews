package vn.evolus.readability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class Readability {	
	public static final String VERSION = "1.7.1";	
	public static final int FLAG_STRIP_UNLIKELYS = 0x1;
    public static final int FLAG_WEIGHT_CLASSES = 0x2;
    public static final int FLAG_CLEAN_CONDITIONALLY = 0x4;
    
    private static final String REGEX_UNLIKELY_CANDIDATES = "(?i)combx|comment|community|disqus|extra|foot|header|menu|remark|rss|shoutbox|sidebar|sponsor|ad-break|agegate|pagination|pager|popup|tweet|twitter";
    private static final String REGEX_OK_MAYBE_ITS_A_CANDIDATE = "(?i)and|article|body|column|main|shadow";
    private static final String REGEX_POSITIVE = "(?i)article|body|content|entry|hentry|main|page|pagination|post|text|blog|story";
    private static final String REGEX_NEGATIVE = "(?i)combx|comment|com-|contact|foot|footer|footnote|masthead|media|meta|outbrain|promo|related|scroll|shoutbox|sidebar|sponsor|shopping|tags|tool|widget";
    private static final String REGEX_DIV_TO_P_ELEMENTS = "(?i)a|blockquote|dl|div|img|ol|p|pre|table|ul";
    private static final String REGEX_KILL_BREAKS = "(?g)(<br\\s*\\/?>(\\s|&nbsp;?)*){1,}";
    private static final String REGEX_VIDEOS = "(?i)http:\\/\\/(www\\.)?(youtube|vimeo)\\.com";
        
    private int flags = FLAG_STRIP_UNLIKELYS | FLAG_WEIGHT_CLASSES | FLAG_CLEAN_CONDITIONALLY; /* Start with all flags set. */    
    private Document document;
    private boolean hasBlockLevelChildren;
    private Map<Element, Double> elementScores = new HashMap<Element, Double>();
    
    public String beautify(String html) {
    	document = Jsoup.parse(html);
        prepareDocument();
        Element articleContent = grabArticle();
        postProcessContent(articleContent);
        return articleContent.outerHtml();
	}
	
	private void prepareDocument() {
		Elements scripts = document.getElementsByTag("script");
        for (int i = scripts.size() - 1; i >= 0; i-=1) {
        	Element script = scripts.get(i);
            script.remove();
        }
        
		Elements styles = document.getElementsByTag("style");
        for (int i = styles.size() - 1; i >= 0; i-=1) {
        	Element style = styles.get(i);
            style.remove();
        }
	}
	
	/***
     * grabArticle - Using a variety of metrics (content score, classname, element types), 
     * find the content that is most likely to be the stuff a user wants to read. 
     * Then return it wrapped up in a div.
     *
     * @param page a document to run upon. Needs to be a full document, complete with body.
     * @return Element
    **/
	private Element grabArticle() {
		// 1. Strip any unlikely candandiates
		List<Element> elementsToScore = stripUnlikelyCandidates();
		
		// 2. Find candidates for article content
		List<Element> candidates = findCandidates(elementsToScore);
		
		// 3. Find the top candidate
		Element topCandidate = findTopCandidate(candidates);
		
		// 4. OK, build the article content
		Element articleContent = buildArticleContent(topCandidate);
		
		// 5. prepare article content
		prepareArticle(articleContent);
				
		return articleContent;
	}

	private void prepareArticle(Element articleContent) {
		cleanStyles(articleContent);
        killBreaks(articleContent);

        /* Clean out junk from the article content */
        cleanConditionally(articleContent, "form");
        clean(articleContent, "object");
        clean(articleContent, "h1");

        /**
         * If there is only one h2, they are probably using it
         * as a header and not a subheader, so remove it since we already have a header.
        ***/
        if (articleContent.getElementsByTag("h2").size() == 1) {
            clean(articleContent, "h2");
        }
        clean(articleContent, "iframe");

        cleanHeaders(articleContent);

        /* Do these last as the previous stuff may have removed junk that will affect these */
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent, "div");

        /* Remove extra paragraphs */
        Elements articleParagraphs = articleContent.getElementsByTag("p");
        for (int i = articleParagraphs.size() - 1; i >= 0; i-=1) {
        	Element p = articleParagraphs.get(i);
            int imgCount    = p.getElementsByTag("img").size();
            int embedCount  = p.getElementsByTag("embed").size();
            int objectCount = p.getElementsByTag("object").size();
            
            if(imgCount == 0 && embedCount == 0 && objectCount == 0 && p.text() == "") {
                p.remove();
            }
        }

        articleContent.html(articleContent.html().replace("(?gi)<br[^>]*>\\s*<p", "<p"));                
	}

	private void cleanHeaders(Element e) {
		for (int headerIndex = 1; headerIndex < 3; headerIndex += 1) {
            Elements headers = e.getElementsByTag("h" + headerIndex);
            for (int i = headers.size() - 1; i >=0; i-=1) {
            	Element header = headers.get(i);
                if (getClassWeight(header) < 0 || getLinkDensity(header) > 0.33) {
                    header.remove();
                }
            }
        }
	}

	/**
     * Clean a node of all elements of type "tag".
     * (Unless it's a youtube/vimeo video. People love movies.)
     *
     * @param Element
     * @param string tag to clean
     * @return void
     **/
	private void clean(Element e, String tag) {
		Elements targetList = e.getElementsByTag(tag);
        boolean isEmbed = tag.equals("object") || tag.equals("embed");
        
        for (Element target : targetList) {
            /* Allow youtube and vimeo videos through as people usually want to see those. */
            if (isEmbed) {
                String attributeValues = "";
                for (Attribute attribute : target.attributes()) {
                    attributeValues += attribute.getValue() + '|';
                }
                
                /* First, check the elements attributes to see if any of them contain youtube or vimeo */
                if (attributeValues.matches(REGEX_VIDEOS)) {
                    continue;
                }

                /* Then check the elements inside this element for the same. */
                if (target.html().matches(REGEX_VIDEOS)) {
                    continue;
                }                
            }

            target.remove();
        }
	}

	/**
     * Clean an element of all tags of type "tag" if they look fishy.
     * "Fishy" is an algorithm based on content length, classnames, link density, number of images & embeds, etc.
     *
     * @return void
     **/
	private void cleanConditionally(Element e, String tagName) {
		if (!flagIsActive(FLAG_CLEAN_CONDITIONALLY)) {
            return;
        }

        Elements tagsList = e.getElementsByTag(tagName);
        int curTagsLength = tagsList.size();

        /**
         * Gather counts for other typical elements embedded within.
         * Traverse backwards so we can remove nodes at the same time without effecting the traversal.
         *
         * TODO: Consider taking into account original contentScore here.
        **/
        for (int i = curTagsLength - 1; i >= 0; i-=1) {        
        	Element tag = tagsList.get(i);
            int weight = getClassWeight(tag);
            double contentScore = getContentScore(tag);
            
            debug("Cleaning Conditionally " + tag + " (" + tag.className() + ":" + tag.id() + ")" + " with score " + contentScore);

            if (weight + contentScore < 0) {
                tag.remove();
            } else if (getCharCount(tag, ",") < 10) {
                /**
                 * If there are not very many commas, and the number of
                 * non-paragraph elements is more than paragraphs or other ominous signs, remove the element.
                **/
                int p      = tag.getElementsByTag("p").size();
                int img    = tag.getElementsByTag("img").size();
                int li     = tag.getElementsByTag("li").size() - 100;
                int input  = tag.getElementsByTag("input").size();

                int embedCount = 0;
                Elements embeds = tag.getElementsByTag("embed");
                for (Element embed : embeds) {
                	String src = embed.attr("src"); 
                    if (src != null && src.matches(REGEX_VIDEOS)) {
                    	embedCount+=1; 
                    }
                }

                double linkDensity = getLinkDensity(tag);
                double contentLength = tag.text().length();
                boolean toRemove = false;

                if ( img > p ) {
                    toRemove = true;
                } else if (li > p && !tagName.equals("ul") && !tagName.equals("ol")) {
                    toRemove = true;
                } else if (input > Math.floor(p/3)) {
                    toRemove = true; 
                } else if (contentLength < 25 && (img == 0 || img > 2) ) {
                    toRemove = true;
                } else if (weight < 25 && linkDensity > 0.2) {
                    toRemove = true;
                } else if (weight >= 25 && linkDensity > 0.5) {
                    toRemove = true;
                } else if((embedCount == 1 && contentLength < 75) || embedCount > 1) {
                    toRemove = true;
                }

                if (toRemove) {
                    tag.remove();
                }
            }
        }
	}

	/**
     * Get the number of times a string s appears in the node e.
     *
     * @param Element
     * @param string - what to split on. Default is ","
     * @return number (integer)
    **/
	private int getCharCount(Element e, String s) {
		if (s == null) s = ",";
        return e.text().split(s).length - 1;		
	}

	/**
     * Remove extraneous break tags from a node.
     *
     * @param Element
     * @return void
     **/
	private void killBreaks(Element e) {
		String html = e.html();
        e.html(html.replace(REGEX_KILL_BREAKS, "<br />"));               
	}

	/**
     * Remove the style attribute on every e and under.
     * TODO: Test if getElementsByTagName(*) is faster.
     *
     * @param Element
     * @return void
    **/
	private void cleanStyles(Element articleContent) {
		traverse(articleContent, new ElementVisitor() {
			public void visit(Element element) {
				if (element.className().equals("readability-styled")) return;
				element.removeAttr("style");
			}			
		});		
	}

	private Element buildArticleContent(Element topCandidate) {
		/**
         * Now that we have the top candidate, look through its siblings for content that might also be related.
         * Things like preambles, content split by ads that we removed, etc.
        **/
        Element articleContent = document.createElement("div");
        double siblingScoreThreshold = Math.max(10.0f, getContentScore(topCandidate) * 0.2f);
        Elements siblingNodes = topCandidate.siblingElements();
        Element nodeToAppend = null;
        
        
        for (int s = 0, sl = siblingNodes.size(); s < sl; s += 1) {
        	Element siblingNode = siblingNodes.get(s);
            boolean append      = false;

            debug("Looking at sibling node: " + siblingNode + " (" + siblingNode.className() + ":" + siblingNode.id() + ")" + " with score " + getContentScore(siblingNode));            

            if (siblingNode == topCandidate) {
                append = true;
            }

            double contentBonus = 0;
            /* Give a bonus if sibling nodes and top candidates have the example same classname */
            if (siblingNode.className().equals(topCandidate.className()) && topCandidate.className().length() != 0) {
                contentBonus += getContentScore(topCandidate) * 0.2;
            }

            if (getContentScore(siblingNode) + contentBonus >= siblingScoreThreshold) {
                append = true;
            }
            
            String tagName = siblingNode.tagName();
            if (tagName.equals("p")) {
                double linkDensity = getLinkDensity(siblingNode);
                String nodeContent = siblingNode.text();
                int nodeLength  = nodeContent.length();
                
                if(nodeLength > 80 && linkDensity < 0.25) {
                    append = true;
                } else if(nodeLength < 80 && linkDensity == 0 && nodeContent.matches("\\.( |$)")) {
                    append = true;
                }
            }

            if (append) {
                debug("Appending node: " + siblingNode);
                
                if (!tagName.equals("div") && tagName.equals("p")) {
                    /* We have a node that isn't a common block level element, like a form or td tag.
                       Turn it into a div so it doesn't get filtered out later by accident. */                    
                    debug("Altering siblingNode of " + tagName + " to div.");
                    nodeToAppend = document.createElement("div");
                   
                    nodeToAppend.attr("id", siblingNode.id());
                    nodeToAppend.html(siblingNode.html());                    
                } else {
                    nodeToAppend = siblingNode;
                    s -= 1;
                    sl -= 1;
                }
                
                /* To ensure a node does not interfere with readability styles, remove its classnames */
                nodeToAppend.removeAttr("class");

                /* Append sibling and subtract from our list because it removes the node when you append to another node */
                articleContent.appendChild(nodeToAppend);
            }
        }
		return articleContent;
	}

	private Element findTopCandidate(List<Element> candidates) {
		/**
         * After we've calculated scores, loop through all of the possible candidate nodes we found
         * and find the one with the highest score.
        **/
        Element topCandidate = null;
        for (Element candidate : candidates) {
            /**
             * Scale the final candidates score based on link density. Good content should have a
             * relatively small link density (5% or less) and be mostly unaffected by this operation.
            **/
        	double contentScore = getContentScore(candidate) * (1 - getLinkDensity(candidate));        	
            setContentScore(candidate, contentScore);

            debug("Candidate: " + candidate + " (" + candidate.className() + ":" + candidate.id() + ") with score " + contentScore);
            
            if (topCandidate == null || contentScore > getContentScore(topCandidate)) {
                topCandidate = candidate; 
            }
        }
        debug("Top candidate: " + topCandidate);
		return topCandidate;
	}

	/**
     * Get the density of links as a percentage of the content
     * This is the amount of text that is inside a link divided by the total text in the node.
     * 
     * @param Element
     * @return number (float)
    **/
	private double getLinkDensity(Element e) {
		Elements links = e.getElementsByTag("a");
        int textLength = e.text().length();
        int linkLength = 0;
        for(Element link : links) {
            linkLength += link.text().length();
        }               
        return textLength == 0 ? 0 : (double)linkLength / textLength;
	}

	/**
     * Loop through all paragraphs, and assign a score to them based on how content-y they look.
     * Then add their score to their parent node.
     *
     * A score is determined by things like number of commas, class names, etc. 
     * Maybe eventually link density.
    **/
	private List<Element> findCandidates(List<Element> elementsToScore) {
		elementScores.clear();		
		
		List<Element> candidates = new ArrayList<Element>();		
        for (Element e : elementsToScore) {
            Element parentNode = e.parent();
            Element grandParentNode = parentNode != null ? parentNode.parent() : null;
            String innerText = e.html();

            if (parentNode == null) {
                continue;
            }

            /* If this paragraph is less than 25 characters, don't even count it. */
            if (innerText.length() < 25) {
                continue; 
            }

            /* Initialize readability data for the parent. */
            if (!elementScores.containsKey(parentNode)) {
            	initializeNode(parentNode);
            	candidates.add(parentNode);
            }

            /* Initialize readability data for the grandparent. */
            if (grandParentNode != null && !elementScores.containsKey(grandParentNode)) {
            	initializeNode(grandParentNode);
            	candidates.add(grandParentNode);
            }

            int contentScore = 0;

            /* Add a point for the paragraph itself as a base. */
            contentScore+=1;

            /* Add points for any commas within this paragraph */
            contentScore += innerText.split(",").length;
            
            /* For every 100 characters in this paragraph, add another point. Up to 3 points. */
            contentScore += Math.min(Math.floor(innerText.length() / 100), 3);
            
            /* Add the score to the parent. The grandparent gets half. */
            setContentScore(parentNode, getContentScore(parentNode) + contentScore);            

            if (grandParentNode != null) {
            	setContentScore(grandParentNode, getContentScore(grandParentNode) + (contentScore/2));                    
            }
        }
        return candidates;
	}

	private void setContentScore(Element element, double score) {
		if (elementScores.containsKey(element)) {
			elementScores.put(element, score);
		}
	}

	private double getContentScore(Element element) {
		if (elementScores.containsKey(element)) {
			return elementScores.get(element);
		}
		return 0;
	}
	
	private void initializeNode(Element node) {
		double contentScore = 0.0f;
        String tagName = node.tagName();
        if (tagName.equals("div")) {
        	contentScore += 5;
        }
        if (tagName.equals("pre")
        	|| tagName.equals("td")
        	|| tagName.equals("blockquote")) {
        	contentScore += 3;
        }
        if (tagName.equals("address")
        	|| tagName.equals("ol")
        	|| tagName.equals("ul")
        	|| tagName.equals("dl")
        	|| tagName.equals("dd")
        	|| tagName.equals("dt")
        	|| tagName.equals("li")
        	|| tagName.equals("form")) {
            contentScore -= 3;
        }
        if (tagName.equals("h1")
        	|| tagName.equals("h2")
        	|| tagName.equals("h3")
        	|| tagName.equals("h4")
        	|| tagName.equals("h5")
        	|| tagName.equals("h6")
        	|| tagName.equals("th")) {
            contentScore -= 5;
        }       
        contentScore += getClassWeight(node);        
        elementScores.put(node, contentScore);
    }

	/**
     * Get an elements class/id weight. Uses regular expressions to tell if this 
     * element looks good or bad.
     *
     * @param Element
     * @return number (Integer)
    **/
	private int getClassWeight(Element e) {
		if (!flagIsActive(FLAG_WEIGHT_CLASSES)) {
            return 0;
        }

        int weight = 0;

        /* Look for a special classname */
        String className = e.className();
        if (className != null && className.length() != 0) {
            if (className.matches(REGEX_NEGATIVE)) {
                weight -= 25; 
            }
            if (className.matches(REGEX_POSITIVE)) {
                weight += 25; 
            }
        }

        /* Look for a special ID */
        String id = e.id();
        if (id != null && id.length() != 0) {
            if (id.matches(REGEX_NEGATIVE)) {
                weight -= 25; 
            }

            if (id.matches(REGEX_POSITIVE)) {
                weight += 25; 
            }
        }

        return weight;		
	}

	/**
     * First, node prepping. Trash nodes that look cruddy (like ones with the class name "comment", etc), 
     * and turn divs into P tags where they have been used inappropriately (as in, 
     * where they contain no other block level elements.)
     *
     * Note: Assignment from index for performance. 
     * See http://www.peachpit.com/articles/article.aspx?p=31567&seqNum=5
     * TODO: Shouldn't this be a reverse traversal?
    **/
	private List<Element> stripUnlikelyCandidates() {
		final boolean stripUnlikely = flagIsActive(FLAG_STRIP_UNLIKELYS);		
		final List<Element> nodesToScore = new ArrayList<Element>();
		traverse(document.body(), new ElementVisitor() {
			public void visit(Element node) {
				String tagName = node.tagName();
				if (stripUnlikely) {
					String unlikelyMatchString = node.className() + node.id();
	                if (unlikelyMatchString.matches(REGEX_UNLIKELY_CANDIDATES) &&
	                	!unlikelyMatchString.matches(REGEX_OK_MAYBE_ITS_A_CANDIDATE) &&	                        
	                    !tagName.equals("body")) {
	                	
	                    debug("Removing unlikely candidate - " + unlikelyMatchString);
	                    node.remove();	                    
	                    return;
	                }
				}
				
				if (tagName.equals("p") || tagName.equals("td") || tagName.equals("pre")) {
	                nodesToScore.add(node);
	            }
				
				/* Turn all divs that don't have children block level elements into p's */								
				hasBlockLevelChildren = false;
	            if (tagName.equals("div")) {
	            	final Element divNode = node;
	            	childTraverse(node, new ElementVisitor() {
	            		public void visit(Element node) {
	            			String tagName = node.tagName();	            			
	            			if (tagName.matches(REGEX_DIV_TO_P_ELEMENTS)) {	            				
	            				Element newNode = document.createElement("p");	            				
	            				newNode.html(divNode.html());
		                        divNode.replaceWith(newNode);
		                        nodesToScore.add(newNode);
		                        hasBlockLevelChildren = true;
	            			}
	            		}
	            	});
	            	
	            	if (!hasBlockLevelChildren) {
	            		for(Node childNode : node.childNodes()) {
	            			if (childNode instanceof TextNode) {
	            				Element p = document.createElement("p");
	            				p.html(((TextNode) childNode).text());
	            				p.attr("style", "display: inline");
	            				p.attr("class", "readability-styled");
	            				childNode.replaceWith(p);
	            			}
	            		}	            		
	            	}
	            }
			}
		});
		return nodesToScore;
	}

	private void postProcessContent(Element articleContent) {
	}
	
	private boolean flagIsActive(int flag) {
		return (this.flags & flag) > 0;
	}
	
	/**
	 * Depth-first traversal
	 * @param rootNode
	 * @param visitor
	 */
	private void traverse(Element element, ElementVisitor visitor) {		
		visitor.visit(element);
		Elements elements = element.children();
		for (Element childElement : elements) {			
			traverse(childElement, visitor);			
		}	    
	}
	
	private void childTraverse(Element element, ElementVisitor visitor) {		
		Elements elements = element.children();
		for (Element childElement : elements) {			
			childTraverse(childElement, visitor);			
		}
	}
	
	private void debug(String message) {
		//System.out.println(message);
	}
}
