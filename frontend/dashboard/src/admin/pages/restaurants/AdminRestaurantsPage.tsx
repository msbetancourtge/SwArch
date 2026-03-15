import { useMemo, useState } from "react";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

type RestaurantStatus = "Pendiente" | "Aprobado" | "Rechazado";

interface AdminRestaurant {
  id: string;
  name: string;
  owner: string;
  email: string;
  status: RestaurantStatus;
  submittedAt: string;
  location: string;
}

const statusColors: Record<RestaurantStatus, string> = {
  Pendiente: "bg-amber-100 text-amber-800",
  Aprobado: "bg-green-100 text-green-800",
  Rechazado: "bg-red-100 text-red-800",
};

const restaurantsSeed: AdminRestaurant[] = [
  {
    id: "RST-3001",
    name: "Urban Bistro",
    owner: "Sarah Johnson",
    email: "sarah.johnson@bistro.com",
    status: "Pendiente",
    submittedAt: "2025-10-20",
    location: "Bogotá, Zona G",
  },
  {
    id: "RST-3002",
    name: "Café Andino",
    owner: "Carlos Mendez",
    email: "carlos.mendez@cafeandino.com",
    status: "Aprobado",
    submittedAt: "2025-09-10",
    location: "Medellín, El Poblado",
  },
  {
    id: "RST-3003",
    name: "Chef House",
    owner: "Luis Ortega",
    email: "luis@chefhouse.com",
    status: "Rechazado",
    submittedAt: "2025-08-15",
    location: "Bogotá, Chapinero",
  },
];

export const AdminRestaurantsPage = () => {
  const [items, setItems] = useState(restaurantsSeed);
  const [statusFilter, setStatusFilter] = useState<RestaurantStatus | "all">("all");
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    return items.filter((rest) => {
      const matchesStatus = statusFilter === "all" || rest.status === statusFilter;
      const q = search.trim().toLowerCase();
      const matchesSearch =
        q.length === 0 ||
        rest.name.toLowerCase().includes(q) ||
        rest.owner.toLowerCase().includes(q) ||
        rest.email.toLowerCase().includes(q) ||
        rest.location.toLowerCase().includes(q);
      return matchesStatus && matchesSearch;
    });
  }, [items, statusFilter, search]);

  const setStatus = (id: string, status: RestaurantStatus) => {
    setItems((prev) => prev.map((r) => (r.id === id ? { ...r, status } : r)));
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900">Restaurantes</h1>
        <p className="text-gray-600">
          Aprobación y control de registros de restaurantes (FR-01.5).
        </p>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-3 md:items-center md:justify-between">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por nombre, dueño, email o ubicación..."
            className="w-full md:w-1/2 rounded-lg border border-gray-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as RestaurantStatus | "all")}
            className="rounded-lg border border-gray-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="all">Todos los estados</option>
            <option value="Pendiente">Pendiente</option>
            <option value="Aprobado">Aprobado</option>
            <option value="Rechazado">Rechazado</option>
          </select>
        </div>

        <Table>
          <TableCaption>Solicitudes pendientes de aprobación, con ubicación y fecha de envío.</TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Restaurante</TableHead>
              <TableHead>Dueño</TableHead>
              <TableHead>Correo</TableHead>
              <TableHead>Ubicación</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Enviado</TableHead>
              <TableHead className="text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((rest) => (
              <TableRow key={rest.id}>
                <TableCell className="font-medium">{rest.id}</TableCell>
                <TableCell>{rest.name}</TableCell>
                <TableCell>{rest.owner}</TableCell>
                <TableCell>{rest.email}</TableCell>
                <TableCell>{rest.location}</TableCell>
                <TableCell>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColors[rest.status]}`}>
                    {rest.status}
                  </span>
                </TableCell>
                <TableCell>{rest.submittedAt}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    {rest.status !== "Aprobado" && (
                      <button
                        className="text-sm px-3 py-1 rounded-lg bg-green-600 text-white hover:bg-green-700"
                        onClick={() => setStatus(rest.id, "Aprobado")}
                      >
                        Aprobar
                      </button>
                    )}
                    {rest.status !== "Rechazado" && (
                      <button
                        className="text-sm px-3 py-1 rounded-lg bg-red-100 text-red-700 hover:bg-red-200"
                        onClick={() => setStatus(rest.id, "Rechazado")}
                      >
                        Rechazar
                      </button>
                    )}
                    {rest.status !== "Pendiente" && (
                      <button
                        className="text-sm px-3 py-1 rounded-lg bg-amber-100 text-amber-800 hover:bg-amber-200"
                        onClick={() => setStatus(rest.id, "Pendiente")}
                      >
                        Marcar pendiente
                      </button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

