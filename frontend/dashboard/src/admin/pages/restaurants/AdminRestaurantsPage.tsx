import { useEffect, useMemo, useState } from "react";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { restaurantService } from "@/lib/services/restaurantService";
import type { Restaurant, RestaurantStatus } from "@/lib/types";

const statusColors: Record<RestaurantStatus, string> = {
  Activo: "bg-green-100 text-green-800",
  Inactivo: "bg-gray-100 text-gray-800",
};

export const AdminRestaurantsPage = () => {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<RestaurantStatus | "all">("all");
  const [cityFilter, setCityFilter] = useState<string>("all");
  const [search, setSearch] = useState("");

  useEffect(() => {
    loadRestaurants();
  }, []);

  const loadRestaurants = async () => {
    setLoading(true);
    try {
      const data = await restaurantService.getAll();
      setRestaurants(data);
    } catch (error) {
      console.error("Error loading restaurants:", error);
    } finally {
      setLoading(false);
    }
  };

  const cities = useMemo(() => {
    return Array.from(new Set(restaurants.map((r) => r.city))).sort((a, b) =>
      a.localeCompare(b),
    );
  }, [restaurants]);

  const filtered = useMemo(() => {
    return restaurants.filter((rest) => {
      const matchesStatus = statusFilter === "all" || rest.status === statusFilter;
      const matchesCity = cityFilter === "all" || rest.city === cityFilter;
      const q = search.trim().toLowerCase();
      const matchesSearch =
        q.length === 0 ||
        rest.id.toLowerCase().includes(q) ||
        rest.name.toLowerCase().includes(q) ||
        rest.category.toLowerCase().includes(q) ||
        rest.city.toLowerCase().includes(q);
      return matchesStatus && matchesCity && matchesSearch;
    });
  }, [restaurants, statusFilter, cityFilter, search]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900">Restaurantes</h1>
        <p className="text-gray-600">
          Listado y estado de restaurantes registrados en la plataforma.
        </p>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-3 md:items-center md:justify-between">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por ID, nombre, categoria o ciudad..."
            className="w-full md:w-1/2 rounded-lg border border-gray-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <div className="flex gap-2">
            <select
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
              className="rounded-lg border border-gray-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">Todas las ciudades</option>
              {cities.map((city) => (
                <option key={city} value={city}>
                  {city}
                </option>
              ))}
            </select>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as RestaurantStatus | "all")}
              className="rounded-lg border border-gray-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">Todos los estados</option>
              <option value="Activo">Activo</option>
              <option value="Inactivo">Inactivo</option>
            </select>
          </div>
        </div>

        <Table>
          <TableCaption>Listado de restaurantes con su categoria, ciudad y estado operativo.</TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Nombre</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead>Ciudad</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Rating</TableHead>
              <TableHead>Creado</TableHead>
              <TableHead className="text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-gray-500">
                  Cargando restaurantes...
                </TableCell>
              </TableRow>
            ) : filtered.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-gray-500">
                  No se encontraron restaurantes con los filtros actuales.
                </TableCell>
              </TableRow>
            ) : (
              filtered.map((rest) => (
                <TableRow key={rest.id}>
                  <TableCell className="font-medium">{rest.id}</TableCell>
                  <TableCell>{rest.name}</TableCell>
                  <TableCell>{rest.category}</TableCell>
                  <TableCell>{rest.city}</TableCell>
                  <TableCell>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColors[rest.status]}`}>
                      {rest.status}
                    </span>
                  </TableCell>
                  <TableCell>{typeof rest.rating === "number" ? rest.rating.toFixed(1) : "—"}</TableCell>
                  <TableCell>{rest.createdAt}</TableCell>
                  <TableCell className="text-right">
                    <button className="text-sm text-blue-600 hover:text-blue-800">Ver</button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

