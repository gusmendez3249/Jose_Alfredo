"""Genera la gráfica burndown de cada sprint con los datos reales del Project "Festival Track".

GitHub Projects solo ofrece gráficas de burn up en Insights; este script calcula el burndown
(puntos pendientes por día) a partir del campo Sprint, el campo Estimate y la fecha de cierre
de cada issue.

Uso (requiere GitHub CLI con sesión iniciada y matplotlib):
    python docs/scrum/herramientas/burndown.py

Genera docs/scrum/sprint-N/img/burndown-sprint-N.png para cada sprint que ya empezó.
"""
import json
import subprocess
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.dates as mdates  # noqa: E402
import matplotlib.pyplot as plt  # noqa: E402

GH = "gh"
PROYECTO_DUENO = "gusmendez3249"
PROYECTO_NUMERO = 2
ZONA_LOCAL = timezone(timedelta(hours=-6))  # Hora del centro de México
PUNTOS_POR_TALLA = {"XS": 1, "S": 2, "M": 3, "L": 5, "XL": 8}
RAIZ_DOCS = Path(__file__).resolve().parents[1]

CONSULTA = """
query($dueno: String!, $numero: Int!) {
  user(login: $dueno) {
    projectV2(number: $numero) {
      items(first: 100) {
        nodes {
          content { ... on Issue { number title closedAt subIssues(first: 1) { totalCount } } }
          sprint: fieldValueByName(name: "Sprint") {
            ... on ProjectV2ItemFieldIterationValue { title startDate duration }
          }
          estimate: fieldValueByName(name: "Estimate") { ... on ProjectV2ItemFieldNumberValue { number } }
          size: fieldValueByName(name: "Size") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
        }
      }
    }
  }
}
"""


def leer_proyecto():
    salida = subprocess.run(
        [GH, "api", "graphql", "-f", f"query={CONSULTA}", "-f", f"dueno={PROYECTO_DUENO}",
         "-F", f"numero={PROYECTO_NUMERO}"],
        capture_output=True, text=True, encoding="utf-8", check=True,
    ).stdout
    return json.loads(salida)["data"]["user"]["projectV2"]["items"]["nodes"]


def puntos(item):
    if item["estimate"] and item["estimate"].get("number") is not None:
        return float(item["estimate"]["number"])
    talla = (item["size"] or {}).get("name")
    return float(PUNTOS_POR_TALLA.get(talla, 1))


def agrupar_por_sprint(items):
    sprints = {}
    for item in items:
        issue, sprint = item["content"], item["sprint"]
        # Las épicas (issues con sub-issues) no suman puntos: sus tareas ya cuentan.
        if not issue or not sprint or issue["subIssues"]["totalCount"] > 0:
            continue
        datos = sprints.setdefault(sprint["title"], {
            "inicio": date.fromisoformat(sprint["startDate"]),
            "duracion": sprint["duration"],
            "tareas": [],
        })
        cierre = None
        if issue["closedAt"]:
            cierre = datetime.fromisoformat(issue["closedAt"].replace("Z", "+00:00")).astimezone(ZONA_LOCAL).date()
        datos["tareas"].append({"numero": issue["number"], "puntos": puntos(item), "cierre": cierre})
    return sprints


def graficar(nombre, datos, hoy):
    inicio = datos["inicio"]
    fin = inicio + timedelta(days=datos["duracion"] - 1)
    dias = [inicio + timedelta(days=i) for i in range(datos["duracion"])]
    total = sum(t["puntos"] for t in datos["tareas"])

    ideal = [total - total * i / (len(dias) - 1) for i in range(len(dias))]
    reales_dias = [d for d in dias if d <= hoy]
    reales = [total - sum(t["puntos"] for t in datos["tareas"] if t["cierre"] and t["cierre"] <= d)
              for d in reales_dias]

    fig, ax = plt.subplots(figsize=(10, 5.2), dpi=120)
    ax.plot(dias, ideal, linestyle="--", color="#8b949e", label="Ideal")
    if reales_dias:
        ax.step(reales_dias, reales, where="post", color="#d4a017", linewidth=2.5, label="Real (puntos pendientes)")
        ax.scatter(reales_dias[-1:], reales[-1:], color="#d4a017", zorder=3)
        ax.annotate(f"{reales[-1]:g} pts", (reales_dias[-1], reales[-1]), textcoords="offset points",
                    xytext=(6, 8), color="#d4a017", fontweight="bold")
    ax.set_title(f"Burndown · {nombre} ({inicio:%d/%m} – {fin:%d/%m/%Y})", fontsize=14, fontweight="bold")
    ax.set_ylabel("Puntos de estimación pendientes")
    ax.set_ylim(0, total * 1.1 if total else 1)
    ax.set_xlim(inicio, fin)
    ax.xaxis.set_major_locator(mdates.DayLocator(interval=1))
    ax.xaxis.set_major_formatter(mdates.DateFormatter("%d/%m"))
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right")
    ax.grid(alpha=0.3)
    ax.legend(loc="upper right")
    cerradas = sum(1 for t in datos["tareas"] if t["cierre"])
    ax.text(0.01, 0.02, f"{cerradas}/{len(datos['tareas'])} tareas cerradas · total {total:g} pts · "
            f"actualizado {hoy:%d/%m/%Y}", transform=ax.transAxes, fontsize=9, color="#57606a")
    fig.tight_layout()

    numero = nombre.split()[-1]
    destino = RAIZ_DOCS / f"sprint-{numero}" / "img" / f"burndown-sprint-{numero}.png"
    destino.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(destino)
    plt.close(fig)
    print(f"{nombre}: {total:g} pts, {cerradas}/{len(datos['tareas'])} cerradas → {destino}")


def main():
    hoy = datetime.now(ZONA_LOCAL).date()
    for nombre, datos in sorted(agrupar_por_sprint(leer_proyecto()).items()):
        if datos["inicio"] <= hoy:
            graficar(nombre, datos, hoy)


if __name__ == "__main__":
    main()
