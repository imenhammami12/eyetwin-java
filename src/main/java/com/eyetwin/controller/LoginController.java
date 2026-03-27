package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.model.User;
import com.eyetwin.service.AuthService;
import com.eyetwin.service.GamingCaptchaService;
import com.eyetwin.service.RememberMeService;
import com.eyetwin.service.TwoFactorAuthService;
import com.eyetwin.dao.UserDAO;
import com.eyetwin.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private CheckBox      rememberMeCheckBox;
    @FXML private WebView       captchaWebView;
    @FXML private Label         captchaStatusLabel;
    @FXML private VBox          captchaContainer;

    private final AuthService          authService          = new AuthService();
    private final GamingCaptchaService gamingCaptchaService = new GamingCaptchaService();
    private final TwoFactorAuthService twoFactorService     = new TwoFactorAuthService(new UserDAO());

    private String     captchaToken  = null;
    private boolean    captchaPassed = false;
    private boolean    captchaReady  = false;
    // Référence forte indispensable : sans ça, le GC Java détruit le bridge
    // après quelques secondes et JS→Java ne fonctionne plus silencieusement
    private JavaBridge javaBridge    = null;

    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        // 1. Charger l'email sauvegardé au démarrage
        String savedEmail = RememberMeService.load();
        if (savedEmail != null) {
            emailField.setText(savedEmail);
            if (rememberMeCheckBox != null) rememberMeCheckBox.setSelected(true);
        }

        // 2. Attacher le listener APRÈS l'initialisation de la checkbox
        if (rememberMeCheckBox != null) {
            rememberMeCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (wasSelected && !isNowSelected) {
                    RememberMeService.clear();
                    System.out.println("Remember Me désactivé — email supprimé");
                }
            });
        }

        loadGamingCaptcha();
    }

    // ─────────────────────────────────────────────
    //  GAMING CAPTCHA
    // ─────────────────────────────────────────────
    private void loadGamingCaptcha() {
        if (captchaWebView == null) return;

        WebEngine engine = captchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    try {
                        javaBridge = new JavaBridge();
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaBridge", javaBridge);
                        engine.executeScript(
                                "if (typeof onBridgeReady === 'function') onBridgeReady();"
                        );
                        captchaReady = true;
                        System.out.println("✅ Gaming Captcha prêt");
                    } catch (Exception e) {
                        System.err.println("❌ Erreur injection bridge: " + e.getMessage());
                    }
                });
            } else if (newState == Worker.State.FAILED) {
                System.err.println("❌ Chargement WebView échoué");
                Platform.runLater(() -> {
                    if (captchaStatusLabel != null) {
                        captchaStatusLabel.setText("Erreur de chargement du captcha.");
                        captchaStatusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11;");
                    }
                });
            }
        });

        engine.loadContent(buildCaptchaHTML());
    }

    // ─────────────────────────────────────────────
    //  JavaBridge
    // ─────────────────────────────────────────────
    public class JavaBridge {

        public void onCaptchaSuccess(String token) {
            Platform.runLater(() -> {
                captchaToken  = token;
                captchaPassed = true;
                if (captchaStatusLabel != null) {
                    captchaStatusLabel.setText("[ OK ]  Verified gamer!");
                    captchaStatusLabel.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-weight: bold; -fx-font-size: 11;");
                }
                System.out.println("✅ Captcha token reçu : " + token);
            });
        }

        public void resizeWebView(double h) {
            Platform.runLater(() -> {
                if (captchaWebView != null) {
                    captchaWebView.setPrefHeight(h);
                    captchaWebView.setMinHeight(Math.min(h, 52));
                }
            });
        }

        public void onCaptchaExpired() {
            Platform.runLater(() -> {
                captchaToken  = null;
                captchaPassed = false;
                if (captchaStatusLabel != null) {
                    captchaStatusLabel.setText("Captcha expired — please redo");
                    captchaStatusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 11;");
                }
            });
        }
    }

    // ─────────────────────────────────────────────
    //  LOGIN — avec 2FA + Trusted Device
    // ─────────────────────────────────────────────
    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        errorLabel.setText("");

        System.out.println("handleLogin() — captchaPassed=" + captchaPassed
                + " | token=" + captchaToken);

        // 1. Vérifier le captcha
        if (!captchaPassed || captchaToken == null) {
            errorLabel.setText("Please complete the gaming verification.");
            return;
        }

        if (!gamingCaptchaService.verify(captchaToken)) {
            errorLabel.setText("Captcha verification failed. Please retry.");
            captchaPassed = false;
            captchaToken  = null;
            loadGamingCaptcha();
            return;
        }

        boolean remember = rememberMeCheckBox != null && rememberMeCheckBox.isSelected();

        try {
            // 2. Vérifier email + password
            User user = authService.login(email, password);
            if (user == null) {
                errorLabel.setText("Invalid email or password.");
                return;
            }

            // 3. Sauvegarder Remember Me
            RememberMeService.save(email, remember);

            // 4. ✅ Vérifier 2FA
            if (twoFactorService.isTwoFactorEnabled(user)) {

                // ✅ Vérifier si l'appareil est de confiance → skip 2FA
                if (SessionManager.isTrustedDevice(user.getId())) {
                    System.out.println("[Login] Appareil de confiance détecté → login direct");
                    SessionManager.completeTwoFactorLogin(user, false);
                    navigateAfterLogin(user);
                    return;
                }

                // Appareil non trusted → afficher la page 2FA
                System.out.println("[Login] 2FA activé → TwoFactorVerify");
                navigateTo2FA(user);

            } else {
                // Pas de 2FA → login direct
                SessionManager.setCurrentUser(user);
                navigateAfterLogin(user);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  Navigation après login réussi
    // ─────────────────────────────────────────────
    private void navigateAfterLogin(User user) {
        if (user.isAdmin()) {
            MainApp.navigateTo("/com/eyetwin/views/dashboard.fxml", "Dashboard");
        } else {
            MainApp.navigateTo("/com/eyetwin/views/home.fxml", "Home");
        }
    }

    // ─────────────────────────────────────────────
    //  Navigation vers 2FA
    // ─────────────────────────────────────────────
    private void navigateTo2FA(User user) {
        try {
            SessionManager.setPending2FAUser(user);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/eyetwin/views/TwoFactorVerify.fxml")
            );
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));

        } catch (Exception e) {
            System.err.println("[Login] Erreur navigation 2FA : " + e.getMessage());
            errorLabel.setText("Erreur lors de la redirection 2FA.");
        }
    }

    // ─────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────
    @FXML public void goToHome()           { MainApp.navigateTo("/com/eyetwin/views/home.fxml",            "Home");           }
    @FXML public void goToRegister()       { MainApp.navigateTo("/com/eyetwin/views/register.fxml",        "Register");       }
    @FXML public void goToForgotPassword() { MainApp.navigateTo("/com/eyetwin/views/forgot-password.fxml", "Reset Password"); }

    // ─────────────────────────────────────────────
    //  buildCaptchaHTML()
    // ─────────────────────────────────────────────
    private String buildCaptchaHTML() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                    :root {
                        --acc:   #ff1a3c;
                        --ag:    rgba(255,26,60,0.14);
                        --as:    rgba(255,26,60,0.28);
                        --bg:    #0d1117;
                        --bdr:   #21262d;
                        --txt:   #c9d1d9;
                        --muted: #484f58;
                        --green: #3fb950;
                        --red:   #f85149;
                        --ora:   #d29922;
                    }
                    * { margin:0; padding:0; box-sizing:border-box; }
                    html,body { width:100%; background:#0a0a0a;
                                font-family:Consolas,'Courier New',monospace; overflow:hidden; }
                    #tz {
                        border:1px solid var(--bdr); border-left:3px solid var(--acc);
                        border-radius:6px; background:var(--ag); padding:11px 14px;
                        display:flex; align-items:center; justify-content:space-between;
                        cursor:pointer; transition:background .2s,border-color .2s; margin:4px 0;
                    }
                    #tz:hover:not(.ok) { background:var(--as); }
                    #tz.ok { border-left-color:var(--green); background:rgba(63,185,80,.07); cursor:default; }
                    .tl  { display:flex; align-items:center; gap:10px; }
                    .chk {
                        width:20px; height:20px; border:2px solid var(--acc); border-radius:4px;
                        display:flex; align-items:center; justify-content:center;
                        font-size:12px; font-weight:bold; transition:all .3s; color:transparent;
                    }
                    #tz.ok .chk { background:var(--green); border-color:var(--green); color:white;
                                   box-shadow:0 0 8px rgba(63,185,80,.5); }
                    .lbl { font-size:13px; color:var(--txt); letter-spacing:.02em; }
                    #tz.ok .lbl { color:var(--green); }
                    .tr  { font-size:8px; color:var(--acc); letter-spacing:.12em;
                            text-align:right; line-height:1.7; }
                    #modal {
                        display:none; position:fixed; inset:0;
                        background:rgba(1,4,9,.96); align-items:center;
                        justify-content:center; z-index:999;
                    }
                    #modal.on { display:flex; }
                    #box {
                        background:var(--bg); border:1px solid var(--bdr);
                        border-top:2px solid var(--acc); border-radius:8px;
                        width:318px; position:relative;
                        animation:pop .26s cubic-bezier(.34,1.56,.64,1);
                    }
                    @keyframes pop{from{transform:scale(.85) translateY(10px);opacity:0}to{transform:scale(1) translateY(0);opacity:1}}
                    .c1,.c2{position:absolute;width:12px;height:12px;}
                    .c1{top:6px;left:6px;border-top:2px solid var(--acc);border-left:2px solid var(--acc);}
                    .c2{bottom:6px;right:6px;border-bottom:2px solid var(--acc);border-right:2px solid var(--acc);}
                    .hdr{display:flex;justify-content:space-between;align-items:center;
                         padding:9px 14px 7px;border-bottom:1px solid var(--bdr);}
                    .htit{font-size:9px;letter-spacing:.22em;color:var(--acc);}
                    .tmr{font-size:14px;color:var(--ora);letter-spacing:.1em;font-weight:bold;}
                    .tmr.d{color:var(--red);animation:bl .5s infinite;}
                    @keyframes bl{0%,100%{opacity:1}50%{opacity:.2}}
                    .pw{height:2px;background:var(--bdr);}
                    .pb{height:100%;background:linear-gradient(90deg,var(--acc),#8b5cf6);transition:width .4s;}
                    .bd{padding:14px;}
                    .cat-badge{
                        display:inline-block; padding:3px 8px; border-radius:3px;
                        font-size:8px; letter-spacing:.15em; font-weight:bold;
                        border:1px solid var(--acc); color:var(--acc);
                        background:var(--ag); margin-bottom:8px;
                    }
                    .cnt{font-size:8px;letter-spacing:.14em;color:var(--muted);margin-bottom:5px;}
                    .qst{font-size:13px;color:var(--txt);font-weight:bold;
                          margin-bottom:13px;min-height:36px;line-height:1.45;
                          border-left:2px solid var(--acc);padding-left:8px;}
                    .opts{display:grid;grid-template-columns:1fr 1fr;gap:5px;}
                    .opt{
                        background:rgba(255,26,60,.06);border:1px solid var(--bdr);
                        border-radius:5px;padding:8px 10px;font-size:11px;color:var(--txt);
                        cursor:pointer;text-align:left;transition:all .14s;
                        font-family:Consolas,'Courier New',monospace;
                    }
                    .opt:hover{border-color:var(--acc);background:var(--as);color:#fff;transform:translateX(2px);}
                    .opt.correct{border-color:var(--green)!important;background:rgba(63,185,80,.12)!important;color:var(--green)!important;}
                    .opt.wrong{border-color:var(--red)!important;background:rgba(248,81,73,.12)!important;color:var(--red)!important;}
                    .ok{font-size:8px;color:var(--acc);margin-right:4px;font-weight:bold;}
                    .dots{display:flex;justify-content:center;gap:6px;margin-top:10px;}
                    .dot{width:9px;height:9px;border-radius:50%;border:1px solid var(--muted);background:transparent;transition:all .3s;}
                    .dot.ok{background:var(--green);border-color:var(--green);box-shadow:0 0 5px rgba(63,185,80,.6);}
                    .dot.fail{background:var(--red);border-color:var(--red);box-shadow:0 0 5px rgba(248,81,73,.6);}
                    .res{text-align:center;padding:8px 0 4px;display:none;}
                    .r-ico{font-size:32px;margin-bottom:6px;letter-spacing:.1em;font-weight:bold;}
                    .r-ico.ok  {color:var(--green);text-shadow:0 0 16px rgba(63,185,80,.6);}
                    .r-ico.fail{color:var(--red);  text-shadow:0 0 16px rgba(248,81,73,.6);}
                    .rtit{font-size:12px;letter-spacing:.22em;margin-bottom:4px;font-weight:bold;}
                    .rtit.ok  {color:var(--green);}
                    .rtit.fail{color:var(--red);}
                    .rsub{font-size:10px;color:var(--muted);}
                    .retry{
                        font-size:9px;letter-spacing:.14em;background:transparent;
                        border:1px solid var(--acc);color:var(--acc);border-radius:4px;
                        padding:5px 12px;cursor:pointer;transition:all .2s;margin-top:8px;
                        font-family:Consolas,'Courier New',monospace;
                    }
                    .retry:hover{background:var(--ag);}
                    .ftr{display:flex;align-items:center;justify-content:space-between;
                         padding:6px 13px;border-top:1px solid var(--bdr);
                         font-size:7px;letter-spacing:.12em;color:var(--muted);}
                    .db{display:inline-block;width:5px;height:5px;border-radius:50%;
                         background:var(--acc);margin-right:4px;animation:dba 1.2s infinite;}
                    @keyframes dba{0%,100%{opacity:1}50%{opacity:.15}}
                </style>
            </head>
            <body>
            <div id="tz" onclick="tryOpen()">
                <div class="tl">
                    <div class="chk" id="chk"></div>
                    <span class="lbl" id="lbl">Click to verify</span>
                </div>
                <div class="tr">GAMING<br>VERIFY</div>
            </div>
            <div id="modal">
                <div id="box">
                    <div class="c1"></div><div class="c2"></div>
                    <div class="hdr">
                        <span class="htit">&gt; GAMING VERIFICATION</span>
                        <span class="tmr" id="tmr">20s</span>
                    </div>
                    <div class="pw"><div class="pb" id="pb" style="width:0%"></div></div>
                    <div class="bd">
                        <div id="game" style="display:none">
                            <div class="cat-badge" id="cat">CATEGORY</div>
                            <div class="cnt" id="cnt">QUESTION 1 / 3</div>
                            <div class="qst" id="qst"></div>
                            <div class="opts" id="opts"></div>
                            <div class="dots" id="dts"></div>
                        </div>
                        <div class="res" id="res">
                            <div class="r-ico" id="rico"></div>
                            <div class="rtit" id="rtit"></div>
                            <div class="rsub" id="rsub"></div>
                            <button class="retry" id="rbtn" onclick="restart()">[ RETRY ]</button>
                        </div>
                    </div>
                    <div class="ftr">
                        <span><span class="db"></span>EYETWIN SECURITY SYSTEM</span>
                        <span>ANTI-BOT v2.0</span>
                    </div>
                </div>
            </div>
            <script>
            const QS = [
                {cat:'[CONTROLS]',  q:'What do you use to play a video game?',
                 o:['A controller','A frying pan','A telescope','A calculator'], a:0},
                {cat:'[MINECRAFT]',  q:'In Minecraft, what do you need to build things?',
                 o:['Sand only','Blocks','Water','Fire'], a:1},
                {cat:'[SLANG]',      q:'What does "GG" mean in gaming?',
                 o:['Go Go','Good Game','Get Gold','Great Goal'], a:1},
                {cat:'[BATTLE ROYALE]', q:'In a Battle Royale, what is the goal?',
                 o:['Build a house','Cook food','Be the last alive','Collect coins'], a:2},
                {cat:'[HUD]',        q:'What does a red heart represent in most games?',
                 o:['Speed','Money','Health / Lives','Ammo'], a:2},
                {cat:'[MARIO]',      q:'In Mario, collecting a star usually makes you...',
                 o:['Slower','Invincible','Lose coins','End the level'], a:1},
                {cat:'[MECHANICS]',  q:"What happens when a player's health reaches zero?",
                 o:['They win','They level up','They die / game over','Nothing'], a:2},
                {cat:'[MULTIPLAYER]',q:'What is a "respawn" in multiplayer games?',
                 o:['Quitting','Coming back after dying','Winning a round','Changing character'], a:1},
                {cat:'[RPG]',        q:'What does armor do in most RPG games?',
                 o:['Makes you faster','Protects from damage','Gives coins','Makes invisible'], a:1},
                {cat:'[ADVENTURE]',  q:'In adventure games, keys are used to...',
                 o:['Attack enemies','Open doors / chests','Heal yourself','Buy items'], a:1},
                {cat:'[NAVIGATION]', q:'What is a "minimap" used for in video games?',
                 o:['Change graphics','See surroundings','Save the game','Change difficulty'], a:1},
                {cat:'[PROGRESS]',   q:'What is a "level up" in a game?',
                 o:['Losing the game','Getting stronger','Game gets easier','Losing a life'], a:1}
            ];
            const TOT=3, TIME=20;
            let cur=0, sc=0, ans=false, tl=TIME, ti=null, sQ=[], bridgeReady=false;
            function onBridgeReady() {
                bridgeReady = true;
                document.getElementById('lbl').textContent = "I'm a real gamer";
            }
            function tryOpen() {
                if (!bridgeReady) return;
                if (document.getElementById('tz').classList.contains('ok')) return;
                pick(); cur=0; sc=0;
                document.getElementById('modal').classList.add('on');
                document.getElementById('game').style.display='block';
                document.getElementById('res').style.display='none';
                try { window.javaBridge.resizeWebView(420); } catch(e){}
                dots(); loadQ();
            }
            function close_() {
                document.getElementById('modal').classList.remove('on');
                clearInterval(ti);
            }
            function closeModal() {
                close_();
                try { window.javaBridge.resizeWebView(52); } catch(e){}
            }
            function pick() {
                sQ = [...QS].sort(() => Math.random() - .5).slice(0, TOT);
            }
            function dots() {
                const r = document.getElementById('dts'); r.innerHTML='';
                for (let i=0;i<TOT;i++) {
                    const d=document.createElement('div');
                    d.className='dot'; d.id='d'+i; r.appendChild(d);
                }
            }
            function loadQ() {
                const q=sQ[cur]; ans=false;
                document.getElementById('res').style.display='none';
                document.getElementById('opts').style.display='grid';
                document.getElementById('cat').textContent=q.cat;
                document.getElementById('cnt').textContent='QUESTION '+(cur+1)+' / '+TOT;
                document.getElementById('qst').textContent=q.q;
                document.getElementById('pb').style.width=((cur/TOT)*100)+'%';
                const ks=['A','B','C','D'], o=document.getElementById('opts');
                o.innerHTML='';
                q.o.forEach((t,i)=>{
                    const b=document.createElement('button');
                    b.type='button'; b.className='opt';
                    b.innerHTML='<span class="ok">['+ks[i]+']</span>'+t;
                    b.onclick=()=>sel(i,b); o.appendChild(b);
                });
                startT();
            }
            function startT() {
                clearInterval(ti); tl=TIME; updT();
                ti=setInterval(()=>{ tl--; updT(); if(tl<=0){clearInterval(ti);if(!ans)tout();}},1000);
            }
            function updT() {
                const e=document.getElementById('tmr');
                e.textContent=tl+'s'; e.className='tmr'+(tl<=5?' d':'');
            }
            function tout() {
                ans=true; mark(cur,false);
                document.querySelectorAll('.opt')[sQ[cur].a].classList.add('correct');
                setTimeout(nxt,900);
            }
            function sel(i,b) {
                if(ans)return; ans=true; clearInterval(ti);
                const ok=i===sQ[cur].a;
                b.classList.add(ok?'correct':'wrong');
                if(!ok) document.querySelectorAll('.opt')[sQ[cur].a].classList.add('correct');
                if(ok) sc++; mark(cur,ok); setTimeout(nxt,850);
            }
            function mark(i,ok) { const d=document.getElementById('d'+i); if(d)d.classList.add(ok?'ok':'fail'); }
            function nxt() { cur++; cur<TOT?loadQ():showRes(); }
            function showRes() {
                document.getElementById('pb').style.width='100%';
                document.getElementById('opts').style.display='none';
                const ok=sc>=2, r=document.getElementById('res');
                r.style.display='block';
                const rico=document.getElementById('rico');
                rico.textContent = ok ? '[ WIN ]' : '[ FAIL ]';
                rico.className   = 'r-ico '+(ok?'ok':'fail');
                document.getElementById('rtit').textContent = ok ? 'ACCESS GRANTED' : 'ACCESS DENIED';
                document.getElementById('rtit').className   = 'rtit '+(ok?'ok':'fail');
                document.getElementById('rsub').textContent = sc+'/'+TOT+' correct -- '+(ok?'Real gamer!':'Try again, rookie!');
                document.getElementById('rbtn').style.display = ok?'none':'inline-block';
                if (ok) {
                    const tz=document.getElementById('tz');
                    tz.classList.add('ok');
                    document.getElementById('chk').textContent='V';
                    document.getElementById('lbl').textContent='Verified gamer!';
                    const tok='GC_'+Date.now()+'_'+sc+'_ok';
                    try { window.javaBridge.onCaptchaSuccess(tok); } catch(e){ console.error(e); }
                    setTimeout(closeModal, 800);
                }
            }
            function restart() {
                pick(); cur=0; sc=0;
                document.getElementById('res').style.display='none';
                document.getElementById('game').style.display='block';
                dots(); loadQ();
            }
            document.getElementById('modal').addEventListener('click', function(e){
                if (e.target === this) closeModal();
            });
            </script>
            </body>
            </html>
            """;
    }
}