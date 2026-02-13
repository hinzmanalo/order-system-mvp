export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  sku: string;
  active: boolean;
  createdAt: string;
}

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
