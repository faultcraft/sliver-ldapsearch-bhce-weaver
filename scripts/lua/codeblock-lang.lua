-- Default unhinted code blocks to "bash" so skylighting can colorize them.

function CodeBlock(el)
  if #el.classes == 0 then
    el.classes:insert("bash")
  end
  return el
end
