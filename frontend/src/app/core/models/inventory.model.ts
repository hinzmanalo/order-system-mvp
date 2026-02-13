export interface InventoryResponse {
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  updatedAt: string;
}

export interface InventoryPage {
  content: InventoryResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface SetStockRequest {
  quantity: number;
}

export interface AdjustStockRequest {
  adjustment: number;
}
