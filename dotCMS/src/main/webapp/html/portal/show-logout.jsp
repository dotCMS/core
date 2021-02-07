<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.util.ReleaseInfo" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="shortcut icon" href="//dotcms.com/favicon.ico" type="image/x-icon">
    <title>dotCMS : <%= LanguageUtil.get(pageContext, "Enterprise-Web-Content-Management") %></title>
    <link rel="stylesheet" type="text/css" href="/html/css/dijit-dotcms/dotcms.css?b=master">
    <style>
        .logout {
            height: 100%;
            width: 100%;
            display: flex;
            justify-content: center;
        }

        .logout__box {
            box-shadow: 0 19px 38px rgba(0, 0, 0, 0.3), 0 15px 12px rgba(0, 0, 0, 0.22);
            background-color: #fff;
            border-radius: 6px;
            align-self: center;
            max-width: 520px;
            width: 90%;
            display: flex;
            justify-content: center;
            padding: 80px 0 64px;
        }

        .logout__container {
            max-width: 360px;
            width: 80%;
        }

        .logout__container h3 {
            font-size: 34px;
            letter-spacing: 0.25px;
            margin: 72px 0 24px 0;
        }

        .logout__container p {
            margin: 0 0 56px 0;
            font-size: 14px;
        }

        .logout__container button {
            background: var(--color-main);
            border: solid 1px transparent;
            height: 36px;
            margin-right: 0;
            outline: none;
            transition: all 150ms ease-in;
            color: #fff;
            font-weight: normal;
            cursor: pointer;
            padding: 0 24px;
            border-radius: 2px;
            font-weight: bold;
            text-transform: uppercase;
            font-size: 13px;
        }

        .logout__container button:hover {
            background: var(--color-main_mod);
        }

        #logout__image {
            width: 102px;
        }
    </style>
</head>

<body>
<div class="logout">
    <div class="logout__box">
        <div class="logout__container">
            <img id="logout__image">
            <h3></h3>
            <p></p>
            <button onclick="goToLogin()"></button>
        </div>
    </div>
</div>

<script>
    const LOGIN_FORM_URL = '/api/v1/loginform';
    fetch(LOGIN_FORM_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Origin: window.location.hostname
        },
        body: JSON.stringify({
            messagesKey: ['sign-in', 'Logout', 'message.successfully.logout'],
            language: '',
            country: ''
        })
    }).then(async response => {
        if (response.status === 200) {
            const data = await response.json();
            const pageBody = document.getElementsByTagName('body')[0];
            const header = document.getElementsByTagName('h3')[0];
            const message = document.getElementsByTagName('p')[0];
            const button = document.getElementsByTagName('button')[0];
            const image = document.getElementById('logout__image');
            pageBody.style.background = `url("${data.entity
                    .backgroundPicture}") top center/cover no-repeat`;
            pageBody.style.backgroundColor = data.entity.backgroundImage;
            image.src = data.entity.logo;
            header.innerText = data.i18nMessagesMap['Logout'];
            message.innerText = data.i18nMessagesMap['message.successfully.logout'];
            button.innerText = data.i18nMessagesMap['sign-in'];
        } else {
            const error = (await response.json()).message
            alert(`Error: ${response.status}. ${error}`);
        }
    });

    function goToLogin() {
        window.location.href = `/dotAdmin/?r=${new Date().getTime()}`;
    }

</script>
</body>
</html>