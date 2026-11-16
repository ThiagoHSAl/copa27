// Simulador de placares: envia o placar assim que as duas caixas estão preenchidas,
// pede os pênaltis quando uma partida de mata-mata empata e devolve o foco à próxima partida.
// Sem JavaScript, cada partida mostra um botão "ok" e os campos de pênaltis ficam sempre visíveis.
(function () {
  "use strict";

  function prontoParaEnviar(form) {
    var gm = form.elements.gm, gv = form.elements.gv;
    if (gm.value === "" || gv.value === "") return false;
    if (form.dataset.mataMata === "1" && gm.value === gv.value) {
      var pm = form.elements.pm, pv = form.elements.pv;
      return pm.value !== "" && pv.value !== "" && pm.value !== pv.value;
    }
    return true;
  }

  function atualizarPenaltis(form) {
    var pen = form.querySelector(".penaltis");
    if (!pen) return;
    var empate = form.elements.gm.value !== "" && form.elements.gm.value === form.elements.gv.value;
    pen.classList.toggle("oculto", !empate);
    if (!empate) {
      form.elements.pm.value = "";
      form.elements.pv.value = "";
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("form.sim-form").forEach(function (form) {
      var temporizador = null;
      atualizarPenaltis(form);

      form.addEventListener("input", function (e) {
        var campo = e.target;
        campo.classList.toggle("preenchida", campo.value !== "");
        atualizarPenaltis(form);
        clearTimeout(temporizador);
        if (campo.value.length === 1) {
          // Avança para a próxima caixa do placar, como em um formulário de palpites.
          var ordem = ["gm", "gv", "pm", "pv"];
          var proximo = ordem[ordem.indexOf(campo.name) + 1];
          var alvo = proximo && form.elements[proximo];
          if (alvo && alvo.offsetParent !== null && alvo.value === "") alvo.focus();
        }
        if (prontoParaEnviar(form)) {
          temporizador = setTimeout(function () { form.requestSubmit(); }, 450);
        }
      });
    });

    var foco = document.querySelector("li.foco input.caixa:not([disabled])");
    if (foco) {
      foco.focus({ preventScroll: true });
      foco.closest("li").scrollIntoView({ block: "center" });
    }
  });
})();
