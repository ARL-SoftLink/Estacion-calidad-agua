(function () {
  'use strict';

  var COLORES_AGUA = {
    LIMPIA: '#bfe3ea', SALADA: '#9fd3c7', TURBIA: '#b89b72',
    MUY_SUCIA: '#5a4632', SIN_MODELO: '#d5dde0'
  };
  var NOMBRES = {
    LIMPIA: 'Agua limpia', SALADA: 'Agua salada', TURBIA: 'Agua turbia',
    MUY_SUCIA: 'Agua muy sucia', SIN_MODELO: 'Sin entrenar'
  };

  var $ = function (id) { return document.getElementById(id); };
  var grafica = null;

  function estadoDe(l) {
    if (l.anomalia) return 2;
    if (l.calidad === 'LIMPIA') return 0;
    if (l.calidad === 'SALADA' || l.calidad === 'TURBIA' || l.calidad === 'SIN_MODELO') return 1;
    return 2;
  }

  function conexion(ok, texto) {
    var el = $('conexion');
    el.textContent = texto;
    el.className = 'conexion ' + (ok ? 'ok' : 'mal');
  }

  function crearGrafica() {
    var ctx = $('grafica').getContext('2d');
    grafica = new Chart(ctx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          { label: 'TDS (ppm)', data: [], borderColor: '#1b8fa6', backgroundColor: '#1b8fa6',
            yAxisID: 'y', pointRadius: 0, borderWidth: 2, tension: .25 },
          { label: 'Turbidez (mV)', data: [], borderColor: '#b98310', backgroundColor: '#b98310',
            yAxisID: 'y1', pointRadius: 0, borderWidth: 2, tension: .25 }
        ]
      },
      options: {
        animation: false, responsive: true, maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        scales: {
          x: { ticks: { maxTicksLimit: 6 } },
          y: { position: 'left', title: { display: true, text: 'TDS (ppm)' } },
          y1: { position: 'right', title: { display: true, text: 'Turbidez (mV)' }, grid: { drawOnChartArea: false } }
        }
      }
    });
  }

  function pintar(datos) {
    if (!datos.length) { return; }
    var u = datos[datos.length - 1];
    var est = estadoDe(u);

    $('liquido').setAttribute('fill', COLORES_AGUA[u.calidad] || COLORES_AGUA.SIN_MODELO);
    $('veredicto').textContent = NOMBRES[u.calidad] || u.calidad;
    var chip = $('estado');
    chip.className = 'estado ' + ['e-verde', 'e-amarillo', 'e-rojo'][est];
    chip.textContent = u.anomalia ? 'Cambio brusco detectado'
      : est === 0 ? 'Dentro de lo esperado' : est === 1 ? 'Revisar la muestra' : 'Alerta';
    $('m-temp').textContent = u.temperatura.toFixed(1);
    $('m-tds').textContent = Math.round(u.tds);
    $('m-mv').textContent = Math.round(u.turbidezMv);
    $('aviso-sim').hidden = !u.simulada;

    grafica.data.labels = datos.map(function (d) { return new Date(d.instante).toLocaleTimeString(); });
    grafica.data.datasets[0].data = datos.map(function (d) { return d.tds; });
    grafica.data.datasets[1].data = datos.map(function (d) { return d.turbidezMv; });
    grafica.update();

    var alertas = datos.filter(function (d) { return d.anomalia; }).slice(-6).reverse();
    var ul = $('lista-eventos');
    ul.innerHTML = '';
    if (!alertas.length) {
      ul.innerHTML = '<li class="vacio">Sin alertas en las últimas lecturas.</li>';
    } else {
      alertas.forEach(function (a) {
        var li = document.createElement('li');
        li.textContent = new Date(a.instante).toLocaleTimeString() + ' – TDS ' + Math.round(a.tds) +
          ' ppm, turbidez ' + Math.round(a.turbidezMv) + ' mV';
        ul.appendChild(li);
      });
    }
  }

  function actualizar() {
    fetch('/api/lecturas/ultimas?n=60')
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (datos) {
        conexion(true, datos.length ? 'Recibiendo lecturas' : 'Servidor activo, sin lecturas aún');
        pintar(datos);
      })
      .catch(function () { conexion(false, 'Sin conexión con el servidor'); });
  }

  function cargarCaptura() {
    fetch('/api/captura')
      .then(function (r) { return r.json(); })
      .then(function (c) {
        $('sel-captura').value = c.etiquetaActiva;
        var ul = $('conteos');
        ul.innerHTML = '';
        Object.keys(c.conteos).forEach(function (k) {
          var li = document.createElement('li');
          li.innerHTML = (NOMBRES[k] || k) + ': <b>' + c.conteos[k] + '</b>';
          ul.appendChild(li);
        });
        $('msg-modelo').textContent = c.muestrasModelo > 0
          ? 'Modelo activo con ' + c.muestrasModelo + ' muestras.'
          : 'Aún no hay modelo: capture al menos 5 muestras de 2 tipos distintos y entrene.';
      })
      .catch(function () { /* se reintenta en el siguiente ciclo */ });
  }

  $('sel-captura').addEventListener('change', function () {
    fetch('/api/captura?etiqueta=' + encodeURIComponent(this.value), { method: 'POST' }).then(cargarCaptura);
  });
  $('btn-entrenar').addEventListener('click', function () {
    fetch('/api/modelo/reentrenar', { method: 'POST' })
      .then(function (r) { return r.json(); })
      .then(function (r) {
        $('msg-modelo').textContent = r.entrenado
          ? 'Modelo entrenado con ' + r.muestrasModelo + ' muestras.'
          : 'No hay datos suficientes: se requieren al menos 5 muestras y 2 tipos distintos.';
        cargarCaptura();
      });
  });

  crearGrafica();
  actualizar();
  cargarCaptura();
  setInterval(actualizar, 2000);
  setInterval(cargarCaptura, 10000);
})();
