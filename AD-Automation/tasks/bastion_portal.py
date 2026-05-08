# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from tasks.dom_utils import click_visible_text_by_dom, print_visible_text_diagnostics
from tasks.remote_drcom_page import select_saved_dx_account

CONNECTION_USERNAME = "dx"



def click_https_entry(page):
    page.wait_for_load_state("domcontentloaded", timeout=15000)
    wait_for_http_protocol_link(page, timeout_ms=30000)
    if not click_http_protocol_link(page):
        click_visible_text_by_dom(page, ["HTTP(S)", "http(s)", "https", "http"], prefer_clickable_ancestor=True)
    print("已点击 http(s) 协议类型")
    page.wait_for_timeout(1500)



def click_connect_button(page):
    wait_for_connect_button(page, timeout_ms=30000)
    select_connection_user(page)
    fill_connection_username(page, CONNECTION_USERNAME)
    connect_page = click_connect_until_new_tab(page)
    print("连接成功，已弹出新标签页:", connect_page.url)
    select_saved_dx_account(connect_page)
    page.wait_for_timeout(1000)



def wait_for_http_protocol_link(page, timeout_ms=30000):
    print("等待资源列表加载 HTTP(S) 协议链接...")
    deadline = timeout_ms
    interval = 500

    while deadline > 0:
        if has_http_protocol_link(page):
            print("已检测到 HTTP(S) 协议链接")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    print_visible_text_diagnostics(page, ["HTTP(S)", "http(s)", "https", "http"])
    raise RuntimeError("等待 HTTP(S) 协议链接超时，资源列表可能还没有加载完成")



def has_http_protocol_link(page):
    for frame in page.frames:
        try:
            exists = frame.evaluate(
                """() => Array.from(document.querySelectorAll('a')).some(link => {
                    const protocol = link.getAttribute('protocol') || '';
                    const title = link.getAttribute('title') || '';
                    const text = (link.innerText || link.textContent || '').trim();
                    const onclick = link.getAttribute('onclick') || '';
                    return protocol.toUpperCase() === 'HTTP(S)'
                        || title.toUpperCase() === 'HTTP(S)'
                        || text.toUpperCase() === 'HTTP(S)'
                        || onclick.includes("'HTTP(S)'");
                })"""
            )
        except Exception:
            exists = False

        if exists:
            return True

    return False



def wait_for_connect_button(page, timeout_ms=30000):
    print("等待右侧连接按钮加载...")
    deadline = timeout_ms
    interval = 500

    while deadline > 0:
        if has_connect_button(page):
            print("已检测到连接按钮")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    print_visible_text_diagnostics(page, ["连接"])
    raise RuntimeError("等待连接按钮超时，右侧连接面板可能还没有加载完成")



def has_connect_button(page):
    for frame in page.frames:
        try:
            exists = frame.evaluate(
                """() => {
                    const target = document.querySelector('a.pl-btn#connect, #connect');
                    if (!target) return false;
                    const style = window.getComputedStyle(target);
                    const rect = target.getBoundingClientRect();
                    return style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && rect.width > 0
                        && rect.height > 0;
                }"""
            )
        except Exception:
            exists = False

        if exists:
            return True

    return False



def click_connect_button_by_dom(page):
    for frame_index, frame in enumerate(page.frames):
        try:
            clicked = frame.evaluate(
                """() => {
                    const target = document.querySelector('a.pl-btn#connect, #connect');
                    if (!target) return false;

                    target.scrollIntoView({ block: 'center', inline: 'center' });
                    const rect = target.getBoundingClientRect();
                    const x = rect.left + rect.width / 2;
                    const y = rect.top + rect.height / 2;
                    for (const eventName of ['mouseover', 'mousedown', 'mouseup', 'click']) {
                        target.dispatchEvent(new MouseEvent(eventName, {
                            bubbles: true,
                            cancelable: true,
                            view: window,
                            clientX: x,
                            clientY: y
                        }));
                    }
                    return true;
                }"""
            )
        except Exception:
            clicked = False

        if clicked:
            print(f"已在 Frame {frame_index} 找到并点击连接按钮")
            return True

    return False



def select_connection_user(page):
    print("准备选择用户")
    deadline = 30000
    interval = 500

    while deadline > 0:
        if click_select_user_by_dom(page):
            print("已点击选择用户")
            page.wait_for_timeout(800)
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    print_visible_text_diagnostics(page, ["选择用户", "用户"])
    raise RuntimeError("等待选择用户按钮超时")



def click_select_user_by_dom(page):
    for frame_index, frame in enumerate(page.frames):
        try:
            clicked = frame.evaluate(
                """() => {
                    const candidates = Array.from(document.querySelectorAll(
                        'button,a,input[type="button"],input[type="submit"],.btn,span,div,[role="button"]'
                    )).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const value = (el.innerText || el.value || el.textContent || el.getAttribute('title') || '').replace(/\\s+/g, '');
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && (value.includes('选择用户') || value === '用户' || value.includes('选择'));
                    });

                    candidates.sort((left, right) => {
                        const leftRect = left.getBoundingClientRect();
                        const rightRect = right.getBoundingClientRect();
                        return rightRect.bottom - leftRect.bottom;
                    });

                    const target = candidates[0];
                    if (!target) return false;
                    clickElement(target.closest('button,a,[role="button"],.btn') || target);
                    return true;

                    function clickElement(el) {
                        el.scrollIntoView({ block: 'center', inline: 'center' });
                        const rect = el.getBoundingClientRect();
                        const x = rect.left + rect.width / 2;
                        const y = rect.top + rect.height / 2;
                        for (const eventName of ['mouseover', 'mousedown', 'mouseup', 'click']) {
                            el.dispatchEvent(new MouseEvent(eventName, {
                                bubbles: true,
                                cancelable: true,
                                view: window,
                                clientX: x,
                                clientY: y
                            }));
                        }
                    }
                }"""
            )
        except Exception:
            clicked = False

        if clicked:
            print(f"已在 Frame {frame_index} 找到并点击选择用户")
            return True

    return False



def fill_connection_username(page, username):
    print("准备填写用户名:", username)
    deadline = 30000
    interval = 500

    while deadline > 0:
        if fill_connection_username_by_dom(page, username):
            print("已填写连接用户名")
            page.wait_for_timeout(500)
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    print_visible_text_diagnostics(page, ["用户名", "用户", "账号"])
    raise RuntimeError("等待用户名输入框超时")



def fill_connection_username_by_dom(page, username):
    for frame_index, frame in enumerate(page.frames):
        try:
            filled = frame.evaluate(
                """username => {
                    const inputs = Array.from(document.querySelectorAll('input, textarea')).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const type = (el.getAttribute('type') || '').toLowerCase();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && type !== 'hidden';
                    });

                    const target = inputs.find(input => {
                        const text = [
                            input.id,
                            input.name,
                            input.placeholder,
                            input.getAttribute('title'),
                            input.getAttribute('aria-label')
                        ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                        return text.includes('user')
                            || text.includes('account')
                            || text.includes('name')
                            || text.includes('用户名')
                            || text.includes('用户')
                            || text.includes('账号');
                    }) || inputs[0];

                    if (!target) return false;
                    target.disabled = false;
                    target.readOnly = false;
                    target.removeAttribute('disabled');
                    target.removeAttribute('readonly');
                    target.focus();
                    target.value = username;
                    for (const eventName of ['keydown', 'input', 'keyup', 'change', 'blur']) {
                        target.dispatchEvent(new Event(eventName, { bubbles: true, cancelable: true }));
                    }
                    return true;
                }""",
                username,
            )
        except Exception:
            filled = False

        if filled:
            print(f"已在 Frame {frame_index} 找到并填写用户名输入框")
            return True

    return False



def click_connect_until_new_tab(page):
    context = page.context
    for attempt in range(1, 5):
        print(f"尝试点击连接按钮（第 {attempt} 次）")
        pages_before = set(context.pages)

        try:
            with context.expect_page(timeout=5000) as new_page_info:
                if not click_connect_button_by_dom(page):
                    click_visible_text_by_dom(page, "连接", prefer_bottom=True, prefer_clickable_ancestor=True)
            new_page = new_page_info.value
            new_page.wait_for_load_state("domcontentloaded", timeout=15000)
            return new_page
        except Exception:
            page.wait_for_timeout(1000)
            click_cancel_dialog_if_present(page)
            page.wait_for_timeout(800)

            new_pages = [candidate for candidate in context.pages if candidate not in pages_before]
            if new_pages:
                new_page = new_pages[-1]
                try:
                    new_page.wait_for_load_state("domcontentloaded", timeout=15000)
                except Exception:
                    pass
                return new_page

            print("本次点击未检测到新标签页，准备重试")

    raise RuntimeError("已多次点击连接，但未检测到新标签页")



def click_cancel_dialog_if_present(page):
    clicked = False
    for frame_index, frame in enumerate(page.frames):
        try:
            frame_clicked = frame.evaluate(
                """() => {
                    const candidates = Array.from(document.querySelectorAll(
                        'button,a,input[type="button"],input[type="submit"],.btn,.layui-layer-btn a,.el-button'
                    )).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const value = (el.innerText || el.value || el.textContent || '').replace(/\\s+/g, '');
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && value === '取消';
                    });

                    const target = candidates[0];
                    if (!target) return false;
                    target.click();
                    return true;
                }"""
            )
        except Exception:
            frame_clicked = False

        if frame_clicked:
            print(f"已在 Frame {frame_index} 点击弹窗取消按钮")
            clicked = True

    if clicked:
        page.wait_for_timeout(800)
    else:
        print("未检测到需要取消的弹窗")

    return clicked



def click_http_protocol_link(page):
    for frame_index, frame in enumerate(page.frames):
        try:
            clicked = frame.evaluate(
                """() => {
                    const links = Array.from(document.querySelectorAll('a'));
                    const target = links.find(link => {
                        const protocol = link.getAttribute('protocol') || '';
                        const title = link.getAttribute('title') || '';
                        const text = (link.innerText || link.textContent || '').trim();
                        const onclick = link.getAttribute('onclick') || '';
                        return protocol.toUpperCase() === 'HTTP(S)'
                            || title.toUpperCase() === 'HTTP(S)'
                            || text.toUpperCase() === 'HTTP(S)'
                            || onclick.includes("'HTTP(S)'");
                    });

                    if (!target) return false;

                    target.scrollIntoView({ block: 'center', inline: 'center' });
                    const rect = target.getBoundingClientRect();
                    const x = rect.left + rect.width / 2;
                    const y = rect.top + rect.height / 2;
                    for (const eventName of ['mouseover', 'mousedown', 'mouseup', 'click']) {
                        target.dispatchEvent(new MouseEvent(eventName, {
                            bubbles: true,
                            cancelable: true,
                            view: window,
                            clientX: x,
                            clientY: y
                        }));
                    }
                    return true;
                }"""
            )
        except Exception:
            clicked = False

        if clicked:
            print(f"已在 Frame {frame_index} 找到并点击 HTTP(S) 协议链接")
            return True

    return False
