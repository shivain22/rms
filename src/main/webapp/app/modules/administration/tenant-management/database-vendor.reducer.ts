import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IDatabaseVendor } from './database-vendor.model';

const initialState = {
  loading: false,
  errorMessage: null,
  databaseVendors: [] as ReadonlyArray<IDatabaseVendor>,
  activeDatabaseVendors: [] as ReadonlyArray<IDatabaseVendor>,
};

export type DatabaseVendorState = Readonly<typeof initialState>;

// Actions
export const getDatabaseVendors = createAsyncThunk('databaseVendor/fetch_all', async (activeOnly: boolean = false) => {
  const requestUrl = activeOnly ? 'api/database-vendors?activeOnly=true' : 'api/database-vendors';
  return axios.get<IDatabaseVendor[]>(requestUrl);
});

export const getDatabaseVendor = createAsyncThunk('databaseVendor/fetch_one', async (id: string | number) => {
  const requestUrl = `api/database-vendors/${id}`;
  return axios.get<IDatabaseVendor>(requestUrl);
});

export const getDatabaseVendorByCode = createAsyncThunk('databaseVendor/fetch_by_code', async (vendorCode: string) => {
  const requestUrl = `api/database-vendors/code/${vendorCode}`;
  return axios.get<IDatabaseVendor>(requestUrl);
});

export const createDatabaseVendor = createAsyncThunk('databaseVendor/create', async (vendor: IDatabaseVendor) => {
  const result = await axios.post<IDatabaseVendor>('api/database-vendors', vendor);
  return result;
});

export const updateDatabaseVendor = createAsyncThunk('databaseVendor/update', async (vendor: IDatabaseVendor) => {
  const result = await axios.put<IDatabaseVendor>(`api/database-vendors/${vendor.id}`, vendor);
  return result;
});

export const deleteDatabaseVendor = createAsyncThunk('databaseVendor/delete', async (id: string | number) => {
  const requestUrl = `api/database-vendors/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const DatabaseVendorSlice = createSlice({
  name: 'databaseVendor',
  initialState: initialState as DatabaseVendorState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getDatabaseVendors.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabaseVendors.fulfilled, (state, action) => {
        state.loading = false;
        const vendors = action.payload.data;
        state.databaseVendors = vendors;
        state.activeDatabaseVendors = vendors.filter(v => v.active !== false);
      })
      .addCase(getDatabaseVendors.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        // Update or add to list
        const vendor = action.payload.data;
        const index = state.databaseVendors.findIndex(v => v.id === vendor.id);
        if (index >= 0) {
          state.databaseVendors[index] = vendor;
        } else {
          state.databaseVendors = [...state.databaseVendors, vendor];
        }
        if (vendor.active !== false) {
          const activeIndex = state.activeDatabaseVendors.findIndex(v => v.id === vendor.id);
          if (activeIndex >= 0) {
            state.activeDatabaseVendors[activeIndex] = vendor;
          } else {
            state.activeDatabaseVendors = [...state.activeDatabaseVendors, vendor];
          }
        }
      })
      .addCase(getDatabaseVendorByCode.fulfilled, (state, action) => {
        state.loading = false;
        const vendor = action.payload.data;
        const index = state.databaseVendors.findIndex(v => v.id === vendor.id);
        if (index >= 0) {
          state.databaseVendors[index] = vendor;
        } else {
          state.databaseVendors = [...state.databaseVendors, vendor];
        }
      })
      .addCase(createDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const vendor = action.payload.data;
        state.databaseVendors = [...state.databaseVendors, vendor];
        if (vendor.active !== false) {
          state.activeDatabaseVendors = [...state.activeDatabaseVendors, vendor];
        }
      })
      .addCase(updateDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const vendor = action.payload.data;
        const index = state.databaseVendors.findIndex(v => v.id === vendor.id);
        if (index >= 0) {
          state.databaseVendors[index] = vendor;
        }
        if (vendor.active !== false) {
          const activeIndex = state.activeDatabaseVendors.findIndex(v => v.id === vendor.id);
          if (activeIndex >= 0) {
            state.activeDatabaseVendors[activeIndex] = vendor;
          } else {
            state.activeDatabaseVendors = [...state.activeDatabaseVendors, vendor];
          }
        } else {
          state.activeDatabaseVendors = state.activeDatabaseVendors.filter(v => v.id !== vendor.id);
        }
      })
      .addCase(deleteDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.databaseVendors = state.databaseVendors.filter(v => v.id !== id);
        state.activeDatabaseVendors = state.activeDatabaseVendors.filter(v => v.id !== id);
      });
  },
});

export const { reset } = DatabaseVendorSlice.actions;

export default DatabaseVendorSlice.reducer;
