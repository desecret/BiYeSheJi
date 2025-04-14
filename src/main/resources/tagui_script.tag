run cmd /c chcp 65001
keyboard [alt][space]
keyboard x
https://www.zwdoctor.com:44502/#/login
wait 3
type images/test/account_input.png as 123
type images/test/password_input.png as 123
if present("images/test/login_button.png")
    click images/test/login_button.png
