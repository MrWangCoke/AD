# Auto-split from campus_portal_task.py. Keep behavior changes focused.





def click_visible_text_by_dom(page, text, prefer_bottom=False, prefer_clickable_ancestor=False, raise_on_missing=True):
    target_texts = text if isinstance(text, list) else [text]
    evaluate_script = (
        """options => {
            const targetTexts = options.targetTexts.map(text => text.replace(/\\s+/g, '').toLowerCase());
            const candidates = Array.from(document.querySelectorAll(
                'button,a,input[type="button"],input[type="submit"],.btn,.verification,span,div,td,th,li,tr,[role="button"]'
            ));

            const visibleCandidates = candidates.filter(el => {
                const style = window.getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                const value = [
                    el.innerText,
                    el.value,
                    el.textContent,
                    el.getAttribute('title'),
                    el.getAttribute('aria-label')
                ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                return style.display !== 'none'
                    && style.visibility !== 'hidden'
                    && rect.width > 0
                    && rect.height > 0
                    && rect.bottom >= 0
                    && rect.top <= window.innerHeight
                    && value.length <= 120
                    && targetTexts.some(targetText => value.includes(targetText));
            });

            const localInput = document.querySelector('#local_v');
            if (options.preferBottom) {
                visibleCandidates.sort((left, right) => {
                    const leftRect = left.getBoundingClientRect();
                    const rightRect = right.getBoundingClientRect();
                    return rightRect.bottom - leftRect.bottom;
                });
            } else if (localInput && visibleCandidates.length > 1) {
                const inputRect = localInput.getBoundingClientRect();
                visibleCandidates.sort((left, right) => {
                    const leftRect = left.getBoundingClientRect();
                    const rightRect = right.getBoundingClientRect();
                    const leftDistance = Math.abs(leftRect.top - inputRect.top) + Math.abs(leftRect.left - inputRect.left);
                    const rightDistance = Math.abs(rightRect.top - inputRect.top) + Math.abs(rightRect.left - inputRect.left);
                    return leftDistance - rightDistance;
                });
            }

            const target = visibleCandidates[0];
            if (!target) return false;
            const clickable = options.preferClickableAncestor
                ? target.closest('button,a,[role="button"],tr,li,.el-table__row,.ant-table-row,.ivu-table-row,.table-row,.resource-item')
                : null;
            (clickable || target).click();
            return true;
        }"""
    )
    options = {
        "targetTexts": target_texts,
        "preferBottom": prefer_bottom,
        "preferClickableAncestor": prefer_clickable_ancestor,
    }

    clicked = False
    for frame in page.frames:
        try:
            clicked = frame.evaluate(evaluate_script, options)
        except Exception:
            clicked = False
        if clicked:
            return True

    if not clicked and raise_on_missing:
        print_visible_text_diagnostics(page, target_texts)
        raise RuntimeError(f"未找到可见的 {' / '.join(target_texts)}")
    return clicked



def print_visible_text_diagnostics(page, target_texts):
    print("未找到目标文本，开始输出当前页面可见文本线索")
    print("目标文本:", " / ".join(target_texts))
    for index, frame in enumerate(page.frames):
        try:
            texts = frame.evaluate(
                """() => Array.from(document.querySelectorAll('body *'))
                    .filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && rect.bottom >= 0
                            && rect.top <= window.innerHeight;
                    })
                    .map(el => (el.innerText || el.value || el.textContent || el.getAttribute('title') || '').trim())
                    .filter(text => text.length > 0 && text.length <= 80)
                    .slice(0, 80)"""
            )
        except Exception as error:
            print(f"Frame {index} 读取失败: {error}")
            continue

        print(f"Frame {index} 地址: {frame.url}")
        if texts:
            print("可见文本:", " | ".join(dict.fromkeys(texts)))
        else:
            print("可见文本: 无")
