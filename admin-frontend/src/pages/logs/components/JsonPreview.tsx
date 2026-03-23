import { useState } from 'react'
import { Button } from '@/components/ui/button'

function formatJson(str: string): string {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

export default function JsonPreview({ label, content }: { label: string; content: string }) {
  const [expanded, setExpanded] = useState(false)
  const formatted = formatJson(content)
  const lines = formatted.split('\n')
  const needCollapse = lines.length > 5
  const preview = needCollapse ? lines.slice(0, 5).join('\n') + '\n...' : formatted

  return (
    <div>
      <span className="text-muted-foreground">{label}：</span>
      <pre className="bg-muted p-2 rounded mt-1 text-xs overflow-x-auto whitespace-pre-wrap break-all max-h-60">
        {needCollapse && !expanded ? preview : formatted}
      </pre>
      {needCollapse && (
        <Button variant="link" size="sm" className="h-6 px-0 text-xs" onClick={() => setExpanded(!expanded)}>
          {expanded ? '收起' : `展开全部 (${lines.length} 行)`}
        </Button>
      )}
    </div>
  )
}
