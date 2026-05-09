# Auto-split from campus_portal_task.py. Keep behavior changes focused.





def click_visible_text_by_dom(
    page,
    text,
    prefer_bottom=False,
    prefer_clickable_ancestor=False,
    raise_on_missing=True,
    prefer_exact_match=False,
):
    target_texts = text if isinstance(text, list) else [text]
    evaluate_script = (
        """options => {
            const targetTexts = options.targetTexts.map(text => text.replace(/\\s+/g, '').toLowerCase());
            const candidates = Array.from(document.querySelectorAll(
                'button,a,input[type="button"],input[type="submit"],.btn,.verification,span,div,td,th,li,tr,[role="button"],[role="menuitem"]'
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
                    && targetTexts.some(targetText => (
                        options.preferExactMatch ? value === targetText : value.includes(targetText)
                    ));
            });

            const localInput = document.querySelector('#local_v');
            const scoreCandidate = element => {
                const value = [
                    element.innerText,
                    element.value,
                    element.textContent,
                    element.getAttribute('title'),
                    element.getAttribute('aria-label')
                ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                const rect = element.getBoundingClientRect();
                let score = 0;

                if (targetTexts.some(targetText => value === targetText)) score += 1000;
                if (targetTexts.some(targetText => value.startsWith(targetText))) score += 300;
                if (targetTexts.some(targetText => value.includes(targetText))) score += 100;
                if (element.matches('a,button,[role="button"],[role="menuitem"],li')) score += 80;
                if (element.className && /menu|nav|item|entry|cell|row|tree/i.test(String(element.className))) score += 60;
                if (rect.width >= 80) score += 20;
                if (rect.height >= 24) score += 20;
                if (options.preferBottom) score += rect.bottom;
                return score;
            };

            visibleCandidates.sort((left, right) => {
                const scoreDiff = scoreCandidate(right) - scoreCandidate(left);
                if (scoreDiff !== 0) return scoreDiff;
                const leftRect = left.getBoundingClientRect();
                const rightRect = right.getBoundingClientRect();
                return leftRect.top - rightRect.top;
            });

            if (!options.preferBottom && localInput && visibleCandidates.length > 1) {
                visibleCandidates.sort((left, right) => {
                    const scoreDiff = scoreCandidate(right) - scoreCandidate(left);
                    if (scoreDiff !== 0) return scoreDiff;
                    const leftRect = left.getBoundingClientRect();
                    const rightRect = right.getBoundingClientRect();
                    const inputRect = localInput.getBoundingClientRect();
                    const leftDistance = Math.abs(leftRect.top - inputRect.top) + Math.abs(leftRect.left - inputRect.left);
                    const rightDistance = Math.abs(rightRect.top - inputRect.top) + Math.abs(rightRect.left - inputRect.left);
                    return leftDistance - rightDistance;
                });
            } else if (options.preferBottom) {
                visibleCandidates.sort((left, right) => {
                    const leftRect = left.getBoundingClientRect();
                    const rightRect = right.getBoundingClientRect();
                    return rightRect.bottom - leftRect.bottom;
                });
            }

            const target = visibleCandidates[0];
            if (!target) return false;
            const clickable = options.preferClickableAncestor
                ? target.closest(
                    'button,a,[role="button"],[role="menuitem"],tr,li,' +
                    '.el-menu-item,.el-submenu__title,.ant-menu-item,.ant-menu-submenu-title,' +
                    '.ivu-menu-item,.ivu-menu-submenu-title,.table-row,.resource-item,.nav-item,.menu-item'
                )
                : null;
            const node = clickable || target;
            node.scrollIntoView({ block: 'center', inline: 'center' });
            const rect = node.getBoundingClientRect();
            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;

            for (const eventName of ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click']) {
                node.dispatchEvent(new MouseEvent(eventName, {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    clientX: x,
                    clientY: y
                }));
            }
            if (typeof node.focus === 'function') node.focus();
            if (typeof node.click === 'function') node.click();
            return true;
        }"""
    )
    options = {
        "targetTexts": target_texts,
        "preferBottom": prefer_bottom,
        "preferClickableAncestor": prefer_clickable_ancestor,
        "preferExactMatch": prefer_exact_match,
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
