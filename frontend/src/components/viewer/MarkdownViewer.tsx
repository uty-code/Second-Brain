import React from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface MarkdownViewerProps {
  content: string;
  onLinkClick?: (linkTarget: string) => void;
}

export function MarkdownViewer({ content, onLinkClick }: MarkdownViewerProps) {
  // 1. Clean up outer markdown code blocks if the file was saved raw as a code block
  let processedContent = content.trim();
  if (processedContent.startsWith("```markdown") && processedContent.endsWith("```")) {
    processedContent = processedContent.slice(11, -3).trim();
  } else if (processedContent.startsWith("```") && processedContent.endsWith("```")) {
    processedContent = processedContent.slice(3, -3).trim();
  }

  // 2. Strip YAML Frontmatter (--- ... ---)
  const frontmatterRegex = /^---\r?\n([\s\S]*?)\r?\n---\r?\n/;
  processedContent = processedContent.replace(frontmatterRegex, "");

  // 3. Parse Wiki links [[target]] or [[target|label]] into markdown links with wiki:// schema
  // URL-encode target to prevent spaces from breaking markdown link parsing
  const wikiLinkRegex = /\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g;
  processedContent = processedContent.replace(wikiLinkRegex, (match, p1, p2) => {
    const target = p1.trim();
    const label = p2 ? p2.trim() : target;
    return `[${label}](wiki://${encodeURIComponent(target)})`;
  });

  // Custom renderer for wiki links
  const components = {
    a: ({ href, children, ...props }: any) => {
      if (href && href.startsWith("wiki://")) {
        // Decode the URL-encoded target
        const target = decodeURIComponent(href.replace("wiki://", ""));
        return (
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              if (onLinkClick) {
                onLinkClick(target);
              }
            }}
            className="text-indigo-400 hover:text-indigo-300 font-semibold no-underline hover:underline cursor-pointer"
            {...props}
          >
            {children}
          </a>
        );
      }
      return (
        <a
          href={href}
          target="_blank"
          rel="noopener noreferrer"
          className="text-indigo-400 hover:text-indigo-300"
          {...props}
        >
          {children}
        </a>
      );
    }
  };

  return (
    <div className="prose prose-invert prose-sm max-w-none prose-zinc leading-relaxed break-words">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={components}
        urlTransform={(url) => url}
      >
        {processedContent}
      </ReactMarkdown>
    </div>
  );
}

