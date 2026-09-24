-- Strip .md file links for PDF output (PDF readers cannot follow .md paths).
-- Preserves anchor-only links, HTTP/HTTPS/FTP/mailto, and non-.md files.

local function is_md_link(target)
    if not target or target == "" then
        return false
    end
    local external_prefixes = {"http://", "https://", "ftp://", "mailto:"}
    for _, prefix in ipairs(external_prefixes) do
        if target:sub(1, #prefix) == prefix then
            return false
        end
    end
    if target:sub(1, 1) == "#" then
        return false
    end
    local path = target:match("^([^#]*)") or target
    return path:match("%.md$") ~= nil
end

function Link(el)
    if is_md_link(el.target) then
        return el.content
    end
    return el
end
